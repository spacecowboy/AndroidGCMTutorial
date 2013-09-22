"""Handle validating that a client is verified"""
from __future__ import print_function
from bottle import request, HTTPError
from httplib2 import Http
import json


def validate_token(access_token):
    '''Verifies that an access-token is valid and
    meant for this app.

    Returns None on fail, and an e-mail on success'''
    h = Http()
    resp, cont = h.request("https://www.googleapis.com/oauth2/v2/userinfo",
                           headers={'Host':'www.googleapis.com',
                                    'Authorization':access_token})

    if not resp['status'] == '200':
        return None

    data = json.loads(cont)

    return data['email']

def gauth(fn):
    """Decorator that checks Bottle requests to
    contain an id-token in the request header.
    userid will be None if the
    authentication failed, and have an id otherwise.

    Use like so:
    bottle.install(guath)"""

    def _wrap(*args, **kwargs):
        if 'Authorization' not in request.headers:
            return HTTPError(401, 'Unauthorized')

        userid = validate_token(request.headers['Authorization'])
        if userid is None:
            return HTTPError(401, "Unauthorized")

        return fn(userid=userid, *args, **kwargs)
    return _wrap
