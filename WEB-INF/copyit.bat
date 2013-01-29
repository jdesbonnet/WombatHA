copy *.xml classes
copy *.properties classes
mkdir classes\META-INF
copy persistence.xml classes\META-INF
del classes\persistence.xml

