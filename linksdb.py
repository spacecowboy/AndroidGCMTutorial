"""Generate a sample project"""

from AndroidCodeGenerator.generator import Generator
from AndroidCodeGenerator.db_table import Table, Column, ForeignKey, Unique
from AndroidCodeGenerator.sql_validator import SQLTester

links = Table('Link').add_cols(Column('sha').text.not_null,
                               Column('url').text.not_null,
                               Column('timestamp').timestamp.not_null\
                               .default_current_timestamp,
                               Column('synced').integer.not_null.default(0))
links.add_constraints(Unique('url').on_conflict_replace,
                      Unique('sha').on_conflict_replace)

s = SQLTester()
s.add_tables(links)
s.test_create()

g = Generator(path='android-client/src/com/nononsenseapps/linksgcm/database',
              pkg='com.nononsenseapps.linksgcm.database')

g.add_tables(links)

g.write()
