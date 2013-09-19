## Basics first - Hello world
This is the first commit: c8c736c87020a

run this and visit [localhost:5500](http://localhost:5500)

```python
python app.py
```

## REST
I'm going to start by creating a REST skeleton of what I
want to do.

__SHA1:__ 76b25c4e3715
```python
import bottle
from bottle import route, run

@route('/')
@route('/links', method='GET')
def list_links():
    '''Return a complete list of all links'''
    return 'List of links'

@route('/links/<id>', method='GET')
def get_link(id):
    '''Returns a specific link'''
    return 'Link {}'.format(id)

@route('/links/<id>', method='DELETE')
def delete_link(id):
    '''Deletes a specific link from the list'''
    return 'Link {} deleted'.format(id)

@route('/links', method='POST')
def add_link():
    '''Adds a link to the list'''
    return 'Link added'

if __name__ == '__main__':
    bottle.debug(True)
    run(port=5500)
```

### JSON
Adding all the methods was really easy. But the REST methods should
return JSON, not strings. So let's tweak it so it returns
dummy JSON data instead.

__SHA1__: 957166a1e4
```python
import bottle
from bottle import route, run

@route('/')
@route('/links', method='GET')
def list_links():
    '''Return a complete list of all links'''
    return dict(links=[])

@route('/links/<id>', method='GET')
def get_link(id):
    '''Returns a specific link'''
    return dict(link={"sha":"1111111",
                      "url":"http://www.google.com",
                      "timestamp":"2013-09-19 08:22:19.000"})

@route('/links/<id>', method='DELETE')
def delete_link(id):
    '''Deletes a specific link from the list.
    On success, returns an empty response'''
    return {}

@route('/links', method='POST')
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
