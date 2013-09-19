#!/bin/sh

echo "Getting all"
curl -X GET http://localhost:5500/links

echo "\n\nDeleting one"
curl -X DELETE http://localhost:5500/links/55

echo "\n\nGetting one"
curl -X GET http://localhost:5500/links/42

echo "\n\nAdding one"
curl -X POST http://localhost:5500/links
