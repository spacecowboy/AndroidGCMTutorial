import bottle
from bottle import route, run

@route('/', method='GET')
def homepage():
    return 'Hello world!'


if __name__ == '__main__':
    bottle.debug(True)
    run(port=5500)
