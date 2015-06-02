# Pal2StarDict
Palauan language in "http://tekinged.com/" to StarDict Converter
```
Usage : java -jar Pal2StarDict.jar mysql-jdbc-url user pass filename [options]
eg) java -jar Pal2StarDict.jar jdbc:mysql://127.0.0.1/palauan root pass /home/kssong/palauan-20150525
        -n : no resources (image, mp3)
        -v : verbose
==> First, /home/kssong/palauan-20150525 directory created
        Second, dict file generated and resource file downloaded to that directory
        And finally, files are zipped to /home/kssong/palauan-20150525.zip
        (directory and files are not deleted for convinience.)
```
