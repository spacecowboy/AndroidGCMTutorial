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
