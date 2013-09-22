from hashlib import sha1
from dateutil import parser as dateparser
from bottle import run, get, post, delete, install, HTTPError, request
from bottle_sqlite import SQLitePlugin

from dbsetup import init_db

from google_auth import gauth

DBNAME='test.db'

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
        request.json['sha'] = sha1(request.json['url']).hexdigest()

    args = [userid,
            request.json['url'],
            request.json['sha']]
    stmt = 'INSERT INTO links (userid, url, sha) VALUES(?, ?, ?)'

    db.execute(stmt, args)

    return get_link(db, request.json['sha'], userid)


if __name__ == '__main__':
    # Restart server automatically when this file changes
    run(host='0.0.0.0', port=5500, reloader=True, debug=True)
