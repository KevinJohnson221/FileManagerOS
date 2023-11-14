# FileManagerOS

Designed to allow the user to organize and manage files/folders that are stored on the Android device (basic funtions include delete, rename, copy, paste, etc.)
Associates an icon with each file based on its type to improve user recognition.
Purpose was to improve data organization and accessibilty.

## Features:
- Allow the user to organize and manage files/folders that are stored on the device. 
- Allow the user to delete, rename, copy, and paste files.
- Allow the user to create, rename, and delete folders, as well as modify the contents of those folders.
- Help the user identify what type of file each file is, based on the icon associated with the file

## OS-Related Low-Level Functionalities:
- File I/O operations (Java I/O API, FileInputStream, FileOutputStream, InputStream, OutputStream, used to read and write files and folders on the device)
- Permissions (ActivityManager API, PackageManager API, request and check for WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE perms to manipulate the storage)
- Environment (Environment class used to get storage location, provides access to environment related info)
- User Interface (Android UI framework, ListView, TextView, Button, EditText, ContextCompat.getDrawable API, ActivityManager API)
