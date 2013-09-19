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
                timestamp=row['timestamp'],
                # Convert integer to boolean
                deleted=(1 == row['deleted']))


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
    db.execute('UPDATE links SET deleted = 1, timestamp = CURRENT_TIMESTAMP \
    WHERE sha IS ? AND userid is ?', [sha, userid])

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
