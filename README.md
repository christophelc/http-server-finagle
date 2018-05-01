# http-server-finagle

This is an example of an http server developped with finagle.

Commands:

#read files
Let suppose there is a server listening at localhost:9000, waiting for a specific token and headers. The initial http request could be:
curl -H 'Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI0NGVhNjUwZi00MmE2LTQ1MzItOGY1YS0wNjdmOGUxYWQ1ODMiLCJuYmYiOjE1MjI3MDY0MDAsImlzcyI6ImVjcmFjIn0.2B7mfJeIfmRdOgeAGFMEyg-DJ9b54qJdIFLulx0-yBc' -H 'X-Api-Version:123' -H 'X-Canal: 456' localhost:9000/api/dossiers

It becomes, passing trough the proxy:
curl localhost:8080/list 

#read one file
curl localhost:8080/dossiers/dossierXYZ

#create a new file
curl -d '{"codeAcprAssureurDO": "111", "refSinistreDO": "1234", "refInterneExpert": "ref", "dateOuvertureChantier": "1970-01-01"}' -H "Content-Type: application/text" -X POST localhost:8080/ouvrir 

#binary
curl -X POST -F file1=@pdf/test.pdf -F file2=@pdf/Annexe.pdf  localhost:8080/dossiers/dossierxy/documents 
curl localhost:8080/dossiers/dossierXYZ/documents/PJ-xyz-3 > a.pdf
