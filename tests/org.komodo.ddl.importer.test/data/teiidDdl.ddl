CREATE FOREIGN TABLE G1( e1 integer, e2 varchar, e3 date, PRIMARY KEY (e1, e2), INDEX(e2, e3), ACCESSPATTERN(e1), UNIQUE(e1), ACCESSPATTERN(e2, e3))