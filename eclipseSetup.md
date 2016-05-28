# How to setup Loklak Server on Eclipse

If you already have a local copy of `loklak_server` repository

1. Download and install [Eclipse](https://eclipse.org/downloads/)
2. Open Eclipse and choose `Import Project`
3. In the window that opens up, Choose `git` and press `Next`
4. Choose a local repository and Add the repository to the search results window.
5. Press Finish
6. Then choose the `loklak_server` git repository in the list and proceed in the wizard.
7. Choose the radio option which says `Import existing Eclipse Projects`
8. Press Finish to open up the Package Explorer and the IDE should be available with the project opened.
9. Once the repository is ready using the Package Explorer navigate to `org.loklak.LoklakServer`
10. Right click on LoklakServer, Choose `Run As > Run Configurations`
11. Choose `Java Application` and press the `New Configuration` button in the top left corner of this pane.
12. In the frame panel that opens up, Move to the Arguments Tab and add the VM Arguments as `-Xmx2G -Xms2G -server -ea`
13. Click `Apply` and then `Close` the window

You can use Eclipse to download the loklak_server from the `git url` in Step 4, Instead of choosing `Local Repository`
choose `Remote URL` and use the git link `https://github.com/loklak/loklak_server.git` and follow the rest of the instructions

# Using the Ant File
1. Choose `File > New > Project`
2. Choose `Java > Java Project from existing ant file`
3. Navigate to the local copy of the repository and use the build file to open up `loklak_server`
