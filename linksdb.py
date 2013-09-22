"""Generate a sample project"""

from AndroidCodeGenerator.generator import Generator
from AndroidCodeGenerator.db_table import (Table, Column, ForeignKey,
                                           Unique, Trigger)
from AndroidCodeGenerator.sql_validator import SQLTester

links = Table('Link').add_cols(Column('sha').text.not_null,
                               Column('url').text.not_null,
                               Column('timestamp').timestamp.not_null\
                               .default_current_timestamp,
                               Column('deleted').integer.not_null\
                               .default(0),
                               Column('synced').integer.not_null\
                               .default(0))

links.add_constraints(Unique('url').on_conflict_ignore,
                      Unique('sha').on_conflict_ignore)

'''
deltrigger = Trigger("tr_del_link").temp.if_not_exists
deltrigger.after.delete_on(links.name)
deltrigger.do_sql("INSERT INTO {table} (sha, url, timestamp, deleted) VALUES\
 (old.sha, old.url, old.timestamp, 1)".format(table=synclinks.name))


intrigger = Trigger("tr_ins_link").temp.if_not_exists
intrigger.after.insert_on(links.name)
intrigger.do_sql("INSERT INTO {table} (sha, url, timestamp) \
VALUES (new.sha, new.url, new.timestamp)"\
                 .format(table=synclinks.name))

#uptrigger = Trigger("tr_upd_link").temp.if_not_exists
#uptrigger.after.update_on(links.name)
#uptrigger.do_sql("INSERT INTO {table} (sha, url, timestamp) \
#VALUES (new.sha, new.url, new.timestamp)"\
#                 .format(table=synclinks.name))
'''

s = SQLTester()
s.add_tables(links)
#s.add_triggers(deltrigger, intrigger)
s.test_create()

g = Generator(path='android-client/src/com/nononsenseapps/linksgcm/database',
              pkg='com.nononsenseapps.linksgcm.database')

g.add_tables(links)
#g.add_triggers(deltrigger, intrigger)

g.write()
