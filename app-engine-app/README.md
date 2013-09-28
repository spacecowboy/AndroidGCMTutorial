## App Engine Server
__SHA1:__ 1be93d88b65c

Please read about the regular server before this. I will not
be writing about every step again in here. What I will do
is note the differences that are necessary due to the restrictions
in App Engine.

## Deployment and Testing
I have included a handy _Makefile_ that does most of what one
wants to do. What you must start with is downloading the
SDK [from here](https://developers.google.com/appengine/downloads#Google_App_Engine_SDK_for_Python).

Then edit the _Makefile_ and change the path to where you
unzipped the SDK. Then you can do the following:

Run it locally:
```
make local
```

Run it locally, but clear out the database first:
```
make clear
```

Deploy it to App Engine proper:
```
make deploy
```

During development, you will be wanting to run it locally. You can
experiment with your API by accessing this page:
[http://localhost:8080/_ah/api/explorer](http://localhost:8080/_ah/api/explorer)

This is probably the biggest advantage of App Engine. Experimenting
with the API this way is a great way to debug your code.

You can access the API explorer for the server running on my App Engine
account at: [esoteric-storm-343.appspot.com/_ah/api/explorer](esoteric-storm-343.appspot.com/_ah/api/explorer)

## The Code
There are a few restrictions due to App Engine's environment. Instead
of Bottle, we are using Google's _Endpoints_ here. In addition,
because we can not install packages in the python environment, I
have manually included _python-gcm_ in the project. An App Engine
app does not have the ability to write to disk so we can't use
the same database solution as before. I have replaced that with
App Engine's _NDB_. The code does not change in a significant
manor.

Every App Engine projects needs a configuration _yaml file_.

__app.yaml:__
```yaml
application: 'Your app engine project name here'
version: 1
runtime: python27
api_version: 1
threadsafe: true

handlers:
- url: /_ah/spi/.*
  script: app.application

libraries:
- name: endpoints
  version: 1.0
# Needed for endpoints/users_id_token.py.
- name: pycrypto
  version: "2.6"
```

### app.py
Because we no longer can use Sqlite, we have to declare some
storage models. We also need to declare the messaging
models so App Engine knows what to convert in the JSON requests.

First, sending a link to the server:
```python
class Link(messages.Message):
    url = messages.StringField(1, required=True)
    sha = messages.StringField(2)
    deleted = messages.BooleanField(3, default=False)
    timestamp = messages.StringField(4)

POST_REQUEST = endpoints.ResourceContainer(
    Link,
    regid=messages.StringField(2))
```

The class _Link_ inherits from _messages.Message_. That means
that all the members defined will be found in the JSON body
of the HTTP-request. *POST_REQUEST* on the other hand
is a _ResourceContainer_ that contains a _Link_. This is the
way to receive URL parameters in the request. _regid_ will
be present in the URL, like in _bob.com/links?regid=123456_.

Similarly, we defined earlier that a delete request would
have the _sha_ of the Link in the URL, e.g.
_bob.com/links/13av25cav31_, which could include a regid also,
_bob.com/links/13av25cav31?regid?123456_.

```python
# Used to request a link to be deleted.
# Has no body, only URL parameter
DELETE_REQUEST = endpoints.ResourceContainer(
    message_types.VoidMessage,
    sha=messages.StringField(2, required=True),
    regid=messages.StringField(3))
```

But as you can see, this actually contains a _VoidMessage_ instead
because a delete request expects no request body. Another
_VoidMessage_ is a request to link all links:

```python
# Used to request the list with query parameters
LIST_REQUEST = endpoints.ResourceContainer(
    message_types.VoidMessage,
    showDeleted=messages.BooleanField(2, default=False),
    timestampMin=messages.StringField(3))
```

Which will respond with a _LinkList_:

```python
class LinkList(messages.Message):
    latestTimestamp = messages.StringField(2)
    links = messages.MessageField(Link, 1, repeated=True)
```

And finally, registering a device for GCM is of the form:

```python
# Add a device id to the user, database model in app_gcm.py
class GCMRegId(messages.Message):
    regid = messages.StringField(1, required=True)
```

Next is the definition of the API. Some basic things are
declared by annotating the API class. It is important
to add all relevant client ids here so that we can use
authentication:

```python
@endpoints.api(name='links', version='v1',
               description='API for Link Management',
               allowed_client_ids=[CLIENT_ID,CLIENT_ID_ANDROID,
                                   endpoints.API_EXPLORER_CLIENT_ID]
               )
class LinkApi(remote.Service):
    '''This is the REST API. Annotations
    specify address, HTTP method and expected
    messages.'''
    pass
```

Authentication is handled by _endpoints_. Just add the following
snippet to the beginning of each method:

```
current_user = endpoints.get_current_user()
if current_user is None:
    raise endpoints.UnauthorizedException('Invalid token.')
```

*current_user* is a UserProperty from which we can acquire the email
of user, but we don't need to. _NDB_ takes care of all that for us.
All we need to do is add a UserProperty to our storage classes, which
incidentally are declared as this:

```python
# Link in NDB
class LinkModel(ndb.Model):
    sha = ndb.StringProperty(required=True)
    url = ndb.StringProperty(required=True)
    deleted =  ndb.BooleanProperty(required=True, default=False)
    userid = ndb.UserProperty(required=True)
    timestamp = ndb.DateTimeProperty(required=True, auto_now=True)

# DeviceID in NDB
class GCMRegIdModel(ndb.Model):
    regid = ndb.StringProperty(required=True)
    userid = ndb.UserProperty(required=True)
```

The API methods that do the actual work look as follows. Adding a link:

```python
    @endpoints.method(POST_REQUEST, Link,
                      name = 'link.insert',
                      path = 'links',
                      http_method = 'POST')
    def add_link(self, request):
        current_user = endpoints.get_current_user()
        if current_user is None:
            raise endpoints.UnauthorizedException('Invalid token.')

        # Generate an ID if one wasn't included
        sha = request.sha
        if sha is None:
            sha = binascii.b2a_hex(os.urandom(15))
        # Construct object to save
        link = LinkModel(key=ndb.Key(LinkModel, sha),
                         sha=sha,
                         url=request.url,
                         deleted=request.deleted,
                         userid=current_user)
        # And save it
        link.put()

        # Notify through GCM
        send_link(link, request.regid)

        # Return a complete link
        return Link(url = link.url,
                    sha = link.sha,
                    timestamp = datetime_to_string(link.timestamp))
```

Deleting a link:

```python
    @endpoints.method(DELETE_REQUEST, message_types.VoidMessage,
                      name = 'link.delete',
                      path = 'links/{sha}',
                      http_method = 'DELETE')
    def delete_link(self, request):
        current_user = endpoints.get_current_user()
        if current_user is None:
            raise endpoints.UnauthorizedException('Invalid token.')

        link_key = ndb.Key(LinkModel, request.sha)
        link = link_key.get()
        if link is not None:
            link.deleted = True
            link.put()
        else:
            raise endpoints.NotFoundException('No such item')

        # Notify through GCM
        send_link(link, request.regid)

        return message_types.VoidMessage()
```

Listing all links:

```python
    @endpoints.method(LIST_REQUEST, LinkList,
                      name = 'link.list',
                      path = 'links',
                      http_method = 'GET')
    def list_links(self, request):
        current_user = endpoints.get_current_user()
        if current_user is None:
            raise endpoints.UnauthorizedException('Invalid token.')

        # Build the query
        q = LinkModel.query(LinkModel.userid == current_user)
        q = q.order(LinkModel.timestamp)

        # Filter on delete
        if not request.showDeleted:
            q = q.filter(LinkModel.deleted == False)

        # Filter on timestamp
        if (request.timestampMin is not None and
            parse_timestamp(request.timestampMin) is not None):
            q = q.filter(LinkModel.timestamp >\
                         parse_timestamp(request.timestampMin))

        # Get the links
        links = []
        latest_time = None
        for link in q:
            ts = link.timestamp
            # Find the latest time
            if latest_time is None:
                latest_time = ts
            else:
                delta = ts - latest_time
                if delta.total_seconds() > 0:
                    latest_time = ts

            # Append to results
            links.append(Link(url=link.url, sha=link.sha,
                              deleted=link.deleted,
                              timestamp=datetime_to_string(ts)))

        if latest_time is None:
            latest_time = datetime(1970, 1, 1, 0, 0)

        return LinkList(links=links,
                        latestTimestamp=datetime_to_string(latest_time))
```

And finally, registering a device for GCM:

```python
    @endpoints.method(GCMRegId, message_types.VoidMessage,
                      name = 'gcm.register',
                      path = 'registergcm',
                      http_method = 'POST')
    def register_gcm(self, request):
        current_user = endpoints.get_current_user()
        if current_user is None:
            raise endpoints.UnauthorizedException('Invalid token.')

        device = GCMRegIdModel(key=ndb.Key(GCMRegIdModel, request.regid),
                               regid=request.regid,
                               userid=current_user)
        # And save it
        device.put()

        # Return nothing
        return message_types.VoidMessage()
```

### app_gcm.py
The only thing that has really changed in the _GCM_ code compared
to the regular server is the retrieval from _NDB_ instead of Sqlite:

```python
def send_link(link, excludeid=None):
    '''Transmits the link specified by the sha to the users devices.

    Does not run in a separate thread because App-Engine did not
    seem to support that.
    '''
    # Get devices
    reg_ids = []
    query = GCMRegIdModel.query(GCMRegIdModel.userid == link.userid)

    for reg_model in query:
        reg_ids.append(reg_model.regid)

    # Dont send to origin device, if specified
    try:
        reg_ids.remove(excludeid)
    except ValueError:
        pass # not in list, or None

    if len(reg_ids) < 1:
        return

    _send(link.userid, reg_ids, to_dict(link))
```

## App Engine vs Regular server
The App Engine version of the server does have a few things
going in its favour.

* API Explorer is great for development
* Your app will scale easily in terms of bandwidth and storage, as long as you're willing to pay for it
* You get https for free
* No need to configure _nginx_ or something else to host the server in a production setting

On the other side, there are some downsides also:

* Developing for App Engine can be frustrating as its Python environment has limits which you might need to work around. You can't use C-extensions
for example.
* Google has the data which might not be what you want because of privacy concerns.

So what to use is a judgement call really. To make an educated decision,
you should investigate the question further elsewhere.
