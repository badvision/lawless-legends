java -jar packr.jar --platform mac --jdk https://www.dropbox.com/s/nnodj0om38bnypk/mac_jre_1.8.151.zip?dl=1 --resources ../jace/target/LawlessLegends.jar --executable LawlessLegends --classpath LawlessLegends.jar --mainclass jace.LawlessLegends --vmargs Xmx1G --output "Lawless Legends.app" --icon game_icon.icns

java -jar packr.jar --platform windows64 --jdk https://www.dropbox.com/s/i4gxp4bhz97j9wx/win_jre_1.8.151.zip?dl=1 --resources ../jace/target/LawlessLegends.jar --executable LawlessLegends --classpath LawlessLegends.jar --mainclass jace.LawlessLegends --vmargs Xmx1G --output "Lawless Legends"

copy /BY game.2mg "Lawless Legends"
copy /BY game.2mg "Lawless Legends.app\Contents\Resources"