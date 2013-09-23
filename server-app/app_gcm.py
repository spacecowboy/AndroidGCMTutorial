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
                # Convert integer to boolean
                deleted=(1 == row['deleted']))


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

    if len(reg_ids) < 1:
        return

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
