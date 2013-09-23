## Basics first - Hello world
__SHA1:__ c8c736c87020a

run this and visit [localhost:5500](http://localhost:5500)

```bash
python app.py
```

## REST
__SHA1:__ d97dc973476324

I'm going to start by creating a REST skeleton of what I
want to do.

```python
import bottle
from bottle import run, get, post, delete

@get('/')
@get('/links')
def list_links():
    '''Return a complete list of all links'''
    return 'List of links'

@get('/links/<id>')
def get_link(id):
    '''Returns a specific link'''
    return 'Link {}'.format(id)

@delete('/links/<id>')
def delete_link(id):
    '''Deletes a specific link from the list'''
    return 'Link {} deleted'.format(id)

@post('/links')
def add_link():
    '''Adds a link to the list'''
    return 'Link added'

if __name__ == '__main__':
    bottle.debug(True)
    run(port=5500)
```

## JSON
__SHA1__: 08c71200b96fc7

Adding all the methods was really easy. But the REST methods should
return JSON, not strings. So let's tweak it so it returns
dummy JSON data instead.

```python
import bottle
from bottle import run, get, post, delete

@get('/')
@get('/links')
def list_links():
    '''Return a complete list of all links'''
    return dict(links=[])

@get('/links/<id>')
def get_link(id):
    '''Returns a specific link'''
    return dict(link={"sha":"1111111",
                      "url":"http://www.google.com",
                      "timestamp":"2013-09-19 08:22:19.000"})

@delete('/links/<id>')
def delete_link(id):
    '''Deletes a specific link from the list.
    On success, returns an empty response'''
    return {}

@post('/links')
def add_link():
    '''Adds a link to the list.
    On success, returns the entry created.'''
    return dict(link={"sha":"1111111",
                      "url":"http://www.google.com",
                      "timestamp":"2013-09-19 08:22:19.000"})

if __name__ == '__main__':
    bottle.debug(True)
    run(port=5500)
```

## Adding a database
__SHA1:__ 599d6fda70fbeaa

Getting the skeleton up was really fast and now it's already
time to implement some real data. The data will be stored
in an sqlite database. The database is really simple and created
in _dbsetup.py:_

```python
import sqlite3 as sql
import sys

_CREATE_TABLE = \
"""CREATE TABLE IF NOT EXISTS links
  (_id INTEGER PRIMARY KEY,
  sha TEXT NOT NULL,
  url TEXT NOT NULL,
  timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  UNIQUE(url) ON CONFLICT REPLACE,
  UNIQUE(sha) ON CONFLICT REPLACE)
"""

def init_db(filename='test.db'):
    con = sql.connect(filename)
    con.row_factory = sql.Row
    with con:
        cur = con.cursor()
        cur.execute(_CREATE_TABLE)

if __name__ == '__main__':
    if len(sys.argv) > 1:
        init_db(sys.argv[1])
    else:
        init_db()
```

To make use of it in our app, we import the *bottle_sqlite* plugin
and add some logic to our existing methods:

```python
from hashlib import sha1
from bottle import run, get, post, delete, install, HTTPError, request
from bottle_sqlite import SQLitePlugin

from dbsetup import init_db


DBNAME='test.db'

init_db(DBNAME)
install(SQLitePlugin(dbfile=DBNAME))

def to_dict(row):
    return dict(sha=row['sha'],
                url=row['url'],
                timestamp=row['timestamp'])

@get('/')
@get('/links')
def list_links(db):
    '''Return a complete list of all links'''
    links = []
    for row in db.execute('SELECT * from links'):
        links.append(to_dict(row))
    return dict(links=links)

@get('/links/<sha>')
def get_link(db, sha):
    '''Returns a specific link'''
    row = db.execute('SELECT * from links WHERE sha IS ?', [sha]).fetchone()
    if row:
        return dict(link=to_dict(row))

    return HTTPError(404, "No such item")

@delete('/links/<sha>')
def delete_link(db, sha):
    '''Deletes a specific link from the list.
    On success, returns an empty response'''
    db.execute('DELETE FROM links WHERE sha IS ?', [sha])
    if db.total_changes > 0:
        return {}

    return HTTPError(404, "No such item")

@post('/links')
def add_link(db):
    '''Adds a link to the list.
    On success, returns the entry created.'''
    # Only accept json data
    if request.content_type != 'application/json':
        return HTTPError(415, "Only json is accepted")
    # Check required fields
    if 'url' not in request.json:
        return HTTPError(400, "Must specify a url")

    # Sha is optional, generate if not present
    if 'sha' not in request.json:
        request.json['sha'] = sha1(request.json['url']).hexdigest()

    args = [request.json['url'],
            request.json['sha']]
    if 'timestamp' in request.json:
        args.append(request.json['timestamp'])
        stmt = 'INSERT INTO links (url, sha, timestamp) VALUES(?, ?, ?)'
    else:
        stmt = 'INSERT INTO links (url, sha) VALUES(?, ?)'

    db.execute(stmt, args)

    return get_link(db, request.json['sha'])


if __name__ == '__main__':
    # Restart server automatically when this file changes
    run(host='0.0.0.0', port=5500, reloader=True, debug=True)
```

Wow. That was fairly straightforward. The one thing that is
missing is a requirement to login.

## Adding Google authentication and users
To make sure we don't mix user's data, we'll add a column in
the database that will hold the username, e.g. their e-mail.

But we also need people to login with Google, and the server
to verify that, so that people can't just use any e-mail
they'd like.

```python
init_db(DBNAME)
install(SQLitePlugin(dbfile=DBNAME))

install(gauth)

def to_dict(row):
    return dict(sha=row['sha'],
                url=row['url'],
                timestamp=row['timestamp'],
                deleted=row['deleted'])


@get('/')
@get('/links')
def list_links(db, userid):
    '''Return a complete list of all links'''
    args = [userid]

    deleted_part = ' AND deleted IS 0'
    if ('showDeleted' in request.query and
        'true' == request.query['showDeleted']):
        deleted_part = ''

    timestamp_part = ''
    if 'timestampMin' in request.query:
        timestamp_part = ' AND timestamp > ?'
        args.append(request.query['timestampMin'])

    latest_time = None
    links = []
    stmt = 'SELECT * from links WHERE userid IS ?'
    stmt += deleted_part + timestamp_part
    for row in db.execute(stmt,
                          args):
        links.append(to_dict(row))
        # Keep track of the latest timestamp here
        if latest_time is None:
            latest_time = row['timestamp']
        else:
            delta = dateparser.parse(row['timestamp']) - dateparser.parse(latest_time)
            if delta.total_seconds() > 0:
                latest_time = row['timestamp']

    return dict(latestTimestamp=latest_time,
                links=links)

@get('/links/<sha>')
def get_link(db, sha, userid):
    '''Returns a specific link'''
    row = db.execute('SELECT * from links WHERE sha IS ? AND userid IS ?',
                     [sha, userid]).fetchone()
    if row:
        return to_dict(row)

    return HTTPError(404, "No such item")

@delete('/links/<sha>')
def delete_link(db, sha, userid):
    '''Deletes a specific link from the list.
    On success, returns an empty response'''
    db.execute('UPDATE links SET deleted = 1, timestamp = CURRENT_TIMESTAMP \
    WHERE sha IS ? AND userid is ?', [sha, userid])

    #if db.total_changes > 0:
    return {}
    #return HTTPError(404, "No such item")

@post('/links')
def add_link(db, userid):
    '''Adds a link to the list.
    On success, returns the entry created.'''
    if 'application/json' not in request.content_type:
        return HTTPError(415, "Only json is accepted")
    # Check required fields
    if ('url' not in request.json or request.json['url'] is None
        or len(request.json['url']) < 1):
        return HTTPError(400, "Must specify a url")

    # Sha is optional, generate if not present
    if 'sha' not in request.json:
        request.json['sha'] = binascii.b2a_hex(os.urandom(15))

    args = [userid,
            request.json['url'],
            request.json['sha']]
    stmt = 'INSERT INTO links (userid, url, sha) VALUES(?, ?, ?)'

    db.execute(stmt, args)

    return get_link(db, request.json['sha'], userid)


if __name__ == '__main__':
    # Restart server automatically when this file changes
    run(host='0.0.0.0', port=5500, reloader=True, debug=True)
```

The methods are only executed if the supplied auth token was valid, else
an _Unauthorized_ exception is thrown.

## Adding GCM
__SHA1:__ 94011e7b6d21f4

CloudMessaging is the final piece of our networked app. By using GCM,
the server can pass a message to Google, and ask it to relay it to the
device(s) at the most oppertune moment. If the device is offline, GCM
queues the message for transmission later when the device comes back.

The way it works is as follows:

1. Device1 uploads a new or deleted link to server as before using the REST API.
2. Server stores the data in the database as before.
3. Server hands the same data to GCM in a request to send to Device2,3,...
4. GCM does its magic and delivers the data to the specified devices.
5. Device2,3... adds or deletes the link from step 1 to their databases

To achieve this on the server side, we are going _python-gcm_ which you can
install by doing:

```shell
pip install python-gcm
```

To avoid blocking the main app, we are also going to do the GCM request on
a separate thread, but this is handled behind the scenes by a decorator.
First though, I created a "config file":

__app_conf.py:__
```python
'''Call from a file which requires the stuff as
from app_conf import GCM_API_KEY
from app_conf import DBNAME'''

DBNAME = 'test.db'
GCM_API_KEY = 'Your key here'
```

Our database needs a table for handling ids for users' devices:

__dbsetup.py:__
```python
import sqlite3 as sql
import sys
from app_conf import DBNAME

_CREATE_TABLE = \
"""CREATE TABLE IF NOT EXISTS links
  (_id INTEGER PRIMARY KEY,
  userid TEXT NOT NULL,
  sha TEXT NOT NULL,
  url TEXT NOT NULL,
  deleted INTEGER NOT NULL DEFAULT 0,
  timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  UNIQUE(userid, url) ON CONFLICT REPLACE,
  UNIQUE(userid, sha) ON CONFLICT REPLACE)
"""

_CREATE_GCM_TABLE = \
"""CREATE TABLE IF NOT EXISTS gcm
  (_id INTEGER PRIMARY KEY,
  userid TEXT NOT NULL,
  regid TEXT NOT NULL,

  UNIQUE(userid, regid) ON CONFLICT REPLACE)
"""

def init_db(filename=DBNAME):
    con = sql.connect(filename)
    con.row_factory = sql.Row
    with con:
        cur = con.cursor()
        cur.execute(_CREATE_TABLE)
        cur.execute(_CREATE_GCM_TABLE)

if __name__ == '__main__':
    if len(sys.argv) > 1:
        init_db(sys.argv[1])
    else:
        init_db()
```

We are only interested in sending newly posted links (and deletes)
through GCM and that is handled here:

__app_gcm.py:__
```python
from __future__ import print_function, division
from threading import Thread
from functools import wraps
import sqlite3 as sql
from gcm import GCM
from app_conf import GCM_API_KEY, DBNAME
from dbsetup import init_db

init_db(DBNAME)

gcm = GCM(GCM_API_KEY)

def to_dict(row):
    return dict(sha=row['sha'],
                url=row['url'],
                timestamp=row['timestamp'],
                deleted=row['deleted'])

def async(func):
    """
    Runs the decorated function in a separate thread.
    Returns the thread.

    Example:
    @async
    def dowork():
        print('Hello from another thread')

    t = dowork()
    t.join()
    """
    @wraps(func)
    def async_func(*args, **kwargs):
        t = Thread(target = func, args = args, kwargs = kwargs)
        t.start()
        return t

    return async_func

@async
def send_link(userid, sha, excludeid=None):
    '''This method runs in a separate thread as to not block
    the main app with this networking IO.

    Transmits the link specified by the sha to the users devices.
    '''
    db = _get_db()
    with db:
        c = db.cursor()
        # Get link
        link = db.execute('SELECT * FROM links WHERE\
        userid IS ? AND sha IS ?', [userid, sha]).fetchone()

        if link is None:
            return

        data = to_dict(link)
        print("Sending data:", data)

        # Get devices
        regrows = db.execute('SELECT * FROM gcm WHERE userid IS ?', [userid])\
                 .fetchall()

        if len(regrows) < 1:
            return

        reg_ids = []
        for row in regrows:
            reg_ids.append(row['regid'])

        # Dont send to origin device, if specified
        try:
            reg_ids.remove(excludeid)
        except ValueError:
            pass # not in list, or None

    print("Sending to:", len(reg_ids))
    _send(userid, reg_ids, data)


def _get_db():
    db = sql.connect(DBNAME)
    db.row_factory = sql.Row
    return db


def _remove_regid(userid, regid):
    db = _get_db()
    with db:
        c = db.cursor()
        c.execute('DELETE FROM gcm WHERE userid IS ? AND regid IS ?',
                  [userid, regid])


def _replace_regid(userid, oldid, newid):
    db = _get_db()
    with db:
        c = db.cursor()
        c.execute('UPDATE gcm SET regid=? WHERE userid IS ? AND regid IS ?',
                  [newid, userid, oldid])


def _send(userid, rids, data):
    '''Send the data using GCM'''
    response = gcm.json_request(registration_ids=rids,
                                data=data,
                                delay_while_idle=True)
    # A device has switched registration id
    if 'canonical' in response:
        for reg_id, canonical_id in response['canonical'].items():
            # Repace reg_id with canonical_id in your database
            _replace_regid(userid, reg_id, canonical_id)

    # Handling errors
    if 'errors' in response:
        for error, reg_ids in response['errors'].items():
            # Check for errors and act accordingly
            if error is 'NotRegistered':
                # Remove reg_ids from database
                for regid in reg_ids:
                    _remove_regid(userid, regid)
```

In our main app, much hasn't changed. The delete and add functions got
an added line each, and we've got a new _REST_ method:

```python
@post('/registergcm')
def register_gcm(db, userid):
    '''Adds a registration id for a user to the database.
    Returns nothing.'''
    if 'application/json' not in request.content_type:
        return HTTPError(415, "Only json is accepted")
    # Check required fields
    if ('regid' not in request.json or request.json['regid'] is None
        or len(request.json['regid']) < 1):
        return HTTPError(400, "Must specify a registration id")

    db.execute('INSERT INTO gcm (userid, regid) VALUES(?, ?)',
               [userid, request.json['regid']])

    if db.total_changes > 0:
        return {}
    else:
        return HTTPError(500, "Adding regid to DB failed")
```

Here is the delete function:

```python
@delete('/links/<sha>')
def delete_link(db, sha, userid):
    '''Deletes a specific link from the list.
    On success, returns an empty response'''
    db.execute('UPDATE links SET deleted = 1, timestamp = CURRENT_TIMESTAMP \
    WHERE sha IS ? AND userid is ?', [sha, userid])

    if db.total_changes > 0:
        # Regid is optional to provide from the client
        # If present, it will not receive a GCM msg
        regid = None
        if 'regid' in request.query:
            regid = request.query['regid']
        send_link(userid, sha, regid)

    return {}
```

Similarly, *send_link* is also called in the add function:

```python
@post('/links')
def add_link(db, userid):
    '''Adds a link to the list.
    On success, returns the entry created.'''
    if 'application/json' not in request.content_type:
        return HTTPError(415, "Only json is accepted")
    # Check required fields
    if ('url' not in request.json or request.json['url'] is None
        or len(request.json['url']) < 1):
        return HTTPError(400, "Must specify a url")

    # Sha is optional, generate if not present
    if 'sha' not in request.json:
        request.json['sha'] = binascii.b2a_hex(os.urandom(15))

    args = [userid,
            request.json['url'],
            request.json['sha']]
    stmt = 'INSERT INTO links (userid, url, sha) VALUES(?, ?, ?)'

    db.execute(stmt, args)

    if db.total_changes > 0:
        # Regid is optional to provide from the client
        # If present, it will not receive a GCM msg
        regid = None
        if 'regid' in request.query:
            regid = request.query['regid']
        send_link(userid, request.json['sha'], regid)

    return get_link(db, request.json['sha'], userid)
```

todo
