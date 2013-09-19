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
