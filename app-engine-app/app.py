import os, binascii
from datetime import datetime

import endpoints
#from google.appengine.ext import endpoints
from google.appengine.ext import ndb
from protorpc import messages
from protorpc import message_types
from protorpc import remote

from app_gcm import send_link, GCMRegIdModel


def datetime_to_string(datetime_object):
    '''Converts a datetime object to a
    timestamp string in the format:

    2013-09-23 23:23:12.123456'''
    return datetime_object.isoformat(sep=' ')

def parse_timestamp(timestamp):
    '''Parses a timestamp string.
    Supports two formats, examples:

    In second precision
    >>> parse_timestamp("2013-09-29 13:21:42")
    datetime object

    Or in fractional second precision (shown in microseconds)
    >>> parse_timestamp("2013-09-29 13:21:42.123456")
    datetime object

    Returns None on failure to parse
    >>> parse_timestamp("2013-09-22")
    None
    '''
    result = None
    try:
        # Microseconds
        result = datetime.strptime(timestamp, '%Y-%m-%d %H:%M:%S.%f')
    except ValueError:
        pass

    try:
        # Seconds
        result = datetime.strptime(timestamp, '%Y-%m-%d %H:%M:%S')
    except ValueError:
        pass

    return result

class Link(messages.Message):
    url = messages.StringField(1, required=True)
    sha = messages.StringField(2)
    deleted = messages.BooleanField(3, default=False)
    timestamp = messages.StringField(4)

POST_REQUEST = endpoints.ResourceContainer(
    Link,
    regid=messages.StringField(2))


class LinkModel(ndb.Model):
    sha = ndb.StringProperty(required=True)
    url = ndb.StringProperty(required=True)
    deleted =  ndb.BooleanProperty(required=True, default=False)
    userid = ndb.UserProperty(required=True)
    timestamp = ndb.DateTimeProperty(required=True, auto_now=True)

# Used to request a link to be deleted.
# Has no body, only URL parameter
DELETE_REQUEST = endpoints.ResourceContainer(
    message_types.VoidMessage,
    sha=messages.StringField(2, required=True),
    regid=messages.StringField(3))

class LinkList(messages.Message):
    latestTimestamp = messages.StringField(2)
    links = messages.MessageField(Link, 1, repeated=True)

# Used to request the list with query parameters
LIST_REQUEST = endpoints.ResourceContainer(
    message_types.VoidMessage,
    showDeleted=messages.BooleanField(2, default=False),
    timestampMin=messages.StringField(3))

# Add a device id to the user, database model in app_gcm.py
class GCMRegId(messages.Message):
    regid = messages.StringField(1, required=True)


# Client id for webapps
CLIENT_ID = '86425096293.apps.googleusercontent.com'
# Client id for devices (android apps)
CLIENT_ID_ANDROID = '86425096293-v1er84h8bmp6c3pcsmdkgupr716u7jha.apps.googleusercontent.com'

@endpoints.api(name='links', version='v1',
               description='API for Link Management',
               allowed_client_ids=[CLIENT_ID,CLIENT_ID_ANDROID,
                                   endpoints.API_EXPLORER_CLIENT_ID]
               )
class LinkApi(remote.Service):
    '''This is the REST API. Annotations
    specify address, HTTP method and expected
    messages.'''

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


if __name__ != "__main__":
    # Set the application for GAE
    application = endpoints.api_server([LinkApi],
                                       restricted=False)
