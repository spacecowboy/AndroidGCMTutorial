#!/bin/sh

echo "Getting all"
curl -X GET http://localhost:5500/links

echo "\n\nDeleting one"
curl -X DELETE http://localhost:5500/links/55

echo "\n\nGetting one"
curl -X GET http://localhost:5500/links/42

echo "\n\nAdding one"
curl -X POST -H "Content-Type: application/json" -d '{"url":"http://www.google.com","username":"xyz","password":"xyz"}' http://localhost:5500/links

echo "\n\nAdding wrong header"
curl -X POST -d '{"username":"xyz","password":"xyz"}' http://localhost:5500/links


echo "\n\nAdding no url"
curl -X POST -H "Content-Type: application/json" -d '{"username":"xyz","password":"xyz"}' http://localhost:5500/links
