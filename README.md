# UnespSisgradCrawler
This is a project in pure java that offers webcrawling functionalities for Unesp's Sisgrad, Parthenon and Lattes (this last one is not exclusive of Unesp). By now, the crawlers only support the basic funcitonalities required in the sisgrad app, with more to come with time. Feel free to contribute by writing new functionalities.


# Building:

Just clone the repository and build with gradle 
```
git clone https://github.com/lucaszanella/UnespSisgradCrawler
cd UnespSisgradCrawler
gradle build
```


# Usage:
Just create a text file named account.txt in /UnespSisgradCrawler/build/classes/main with the following:

```
user=your_user
password=your_password
```

Then run Main to see how it works. 
