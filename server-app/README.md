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

### Creating a project with Google
Create projects in api console...

todo
