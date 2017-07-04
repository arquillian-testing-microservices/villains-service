
#!/bin/sh

var=$(jq -n --arg v "villains:$1" '[{"op": "replace", "path":"/spec/template/spec/containers/0/image","value": $v}]')

oc patch dc villains --type=json -p="${var}"
#oc rollout latest dc/crimes -n villains