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
