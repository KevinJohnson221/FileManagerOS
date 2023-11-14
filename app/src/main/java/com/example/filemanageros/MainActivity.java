package com.example.filemanageros;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    private File[] files;
    private File directory;
    private List<String> filesList;
    private boolean[] selection;
    private boolean isFileManagerInitialized;
    private TextAdapter textAdapter;
    private Button refreshButton;
    private String currentPath;
    private String copyPath;
    private boolean isLongClick;
    private int selectedItemIndex;
    private int filesFoundCount;
    private static final int REQUEST_PERMISSIONS = 1234;
    private static final String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private static final int PERMISSIONS_COUNT = 2;

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onResume() {
        super.onResume();
        if (arePermissionsDenied()) {
            requestPermissions(PERMISSIONS, REQUEST_PERMISSIONS);
            return;
        }

        if (!isFileManagerInitialized) {
            currentPath = String.valueOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
            final String rootPath = currentPath.substring(0, currentPath.lastIndexOf('/'));
            currentPath = rootPath;

            final TextView pathOutput = findViewById(R.id.pathOutput);

            final ListView listView = findViewById(R.id.listView);
            textAdapter = new TextAdapter();
            listView.setAdapter(textAdapter);
            filesList = new ArrayList<>();

            refreshButton = findViewById(R.id.refresh);
            refreshButton.setOnClickListener(view -> {
                pathOutput.setText(currentPath.substring(currentPath.lastIndexOf('/') + 1));
                directory = new File(currentPath);
                files = directory.listFiles();
                filesFoundCount = files.length;
                selection = new boolean[filesFoundCount];
                textAdapter.setSelection(selection);
                filesList.clear();
                for (int i = 0; i < filesFoundCount; i++) {
                    filesList.add(files[i].getAbsolutePath());
                }
                textAdapter.setData(filesList);
            });

            refreshButton.callOnClick();

            final Button backButton = findViewById(R.id.back);
            backButton.setOnClickListener(view -> {
                if (currentPath.equals(rootPath)) {
                    return;
                }
                currentPath = currentPath.substring(0, currentPath.lastIndexOf('/'));
                refreshButton.callOnClick();
            });

            listView.setOnItemClickListener((adapterView, view, i, l) -> new Handler().postDelayed(() -> {
                if (!isLongClick) {
                    if (files[i].isDirectory()) {
                        currentPath = files[i].getAbsolutePath();
                        refreshButton.callOnClick();
                    }
                }
            }, 50));

            listView.setOnItemLongClickListener((adapterView, view, i, l) -> {
                isLongClick = true;
                selection[i] = !selection[i];
                textAdapter.setSelection(selection);
                int selectionCount = 0;
                for (boolean aSelection : selection) {
                    if (aSelection) {
                        selectionCount++;
                    }
                }
                if (selectionCount > 0) {
                    if (selectionCount == 1) {
                        selectedItemIndex = i;
                        findViewById(R.id.rename).setVisibility(View.VISIBLE);
                        if (!files[selectedItemIndex].isDirectory()) {
                            findViewById(R.id.copy).setVisibility(View.VISIBLE);
                        }
                    } else {
                        findViewById(R.id.copy).setVisibility(View.GONE);
                        findViewById(R.id.rename).setVisibility(View.GONE);
                    }
                    findViewById(R.id.bottomBar).setVisibility(View.VISIBLE);
                } else {
                    findViewById(R.id.bottomBar).setVisibility(View.GONE);
                }
                new Handler().postDelayed(() -> isLongClick = false, 1000);
                return false;
            });

            final Button newFolderButton = findViewById(R.id.newFolder);
            newFolderButton.setOnClickListener(view -> {
                final AlertDialog.Builder newFolderDialog = new AlertDialog.Builder(MainActivity.this);
                newFolderDialog.setTitle("New Folder");
                final EditText input = new EditText(MainActivity.this);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                newFolderDialog.setView(input);
                newFolderDialog.setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.cancel());
                newFolderDialog.setPositiveButton("Ok", (dialogInterface, i) -> {
                    final File newFolder = new File(currentPath + "/" + input.getText());
                    if (!newFolder.exists()) {
                        newFolder.mkdir();
                        refreshButton.callOnClick();
                    }
                });
                newFolderDialog.show();
            });

            final Button deleteButton = findViewById(R.id.delete);
            deleteButton.setOnClickListener(view -> {
                final AlertDialog.Builder deleteDialog = new AlertDialog.Builder(MainActivity.this);
                deleteDialog.setTitle("Delete");
                deleteDialog.setMessage("Are you sure you want to delete this?");
                deleteDialog.setNegativeButton("No", (dialogInterface, i) -> {
                    dialogInterface.cancel();
                    refreshButton.callOnClick();
                });
                deleteDialog.setPositiveButton("Yes", (dialogInterface, i) -> {
                    for (int z = 0; z < files.length; z++) {
                        if (selection[z]) {
                            deleteFileOrFolder(files[z]);
                            selection[z] = false;
                        }
                    }
                    refreshButton.callOnClick();
                });
                deleteDialog.show();
            });

            final Button renameButton = findViewById(R.id.rename);
            renameButton.setOnClickListener(view -> {
                final AlertDialog.Builder renameDialog = new AlertDialog.Builder(MainActivity.this);
                renameDialog.setTitle("Rename to:");
                final EditText input = new EditText(MainActivity.this);
                final String renamePath = files[selectedItemIndex].getAbsolutePath();
                input.setText(renamePath.substring(renamePath.lastIndexOf('/')));
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                renameDialog.setView(input);
                renameDialog.setPositiveButton("Rename", (dialogInterface, i) -> {
                    String s = new File(renamePath).getParent() + '/' + input.getText();
                    File newFile = new File(s);
                    new File(renamePath).renameTo(newFile);
                    refreshButton.callOnClick();
                    selection = new boolean[files.length];
                    textAdapter.setSelection(selection);
                });
                renameDialog.setNegativeButton("Cancel", (dialogInterface, i) -> {
                    dialogInterface.cancel();
                    refreshButton.callOnClick();
                });
                renameDialog.show();
            });

            final Button copyButton = findViewById(R.id.copy);
            copyButton.setOnClickListener(view -> {
                if (files[selectedItemIndex].isDirectory()) {
                    refreshButton.callOnClick();
                    return;
                }
                copyPath = files[selectedItemIndex].getAbsolutePath();
                selection = new boolean[files.length];
                textAdapter.setSelection(selection);
                findViewById(R.id.paste).setVisibility(View.VISIBLE);
            });

            final Button pasteButton = findViewById(R.id.paste);
            pasteButton.setOnClickListener(view -> {
                pasteButton.setVisibility(View.GONE);
                String destinationPath = currentPath + copyPath.substring(copyPath.lastIndexOf('/'));

                copy(new File(copyPath), new File(destinationPath));
                refreshButton.callOnClick();
            });

            isFileManagerInitialized = true;
        } else {
            refreshButton.callOnClick();
        }
    }

    private void copy(File src, File dst) {
        try {
            InputStream inputStream = new FileInputStream(src);
            OutputStream outputStream = new FileOutputStream(dst);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.close();
            inputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    static class TextAdapter extends BaseAdapter {

        final private List<String> data = new ArrayList<>();

        private boolean[] selection;

        public void setData(List<String> data) {
            if (data != null) {
                this.data.clear();
                if (data.size() > 0) {
                    this.data.addAll(data);
                }
                notifyDataSetChanged();
            }
        }

        @SuppressWarnings("ManualArrayCopy")
        void setSelection(boolean[] selection) {
            if (selection != null) {
                this.selection = new boolean[selection.length];
                for (int c = 0; c < selection.length; c++) {
                    this.selection[c] = selection[c];
                }
                notifyDataSetChanged();
            }
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public String getItem(int i) {
            return data.get(i);
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item, viewGroup, false);
                view.setTag(new ViewHolder(view.findViewById(R.id.textItem)));
            }
            ViewHolder holder = (ViewHolder) view.getTag();
            final String item = getItem(i);
            holder.information.setText(item.substring(item.lastIndexOf('/') + 1));

            Drawable drawable;
            if (item.endsWith(".jpeg") || item.endsWith(".jpg") || item.endsWith(".png")) {
                drawable = ContextCompat.getDrawable(view.getContext(), R.drawable.ic_image);
            } else if (item.endsWith(".pdf") || item.endsWith(".txt")) {
                drawable = ContextCompat.getDrawable(view.getContext(), R.drawable.ic_text);
            } else if (item.endsWith(".doc") || item.endsWith(".docx")) {
                drawable = ContextCompat.getDrawable(view.getContext(), R.drawable.ic_doc);
            } else if (item.endsWith(".ppt") || item.endsWith(".pptx")) {
                drawable = ContextCompat.getDrawable(view.getContext(), R.drawable.ic_slides);
            } else if (item.endsWith(".mp3") || item.endsWith(".wav")) {
                drawable = ContextCompat.getDrawable(view.getContext(), R.drawable.ic_audio);
            } else if (item.endsWith(".mp4") || item.endsWith(".mov")) {
                drawable = ContextCompat.getDrawable(view.getContext(), R.drawable.ic_video);
            } else if (item.endsWith(".apk") || item.endsWith(".xml") || item.endsWith(".java")) {
                drawable = ContextCompat.getDrawable(view.getContext(), R.drawable.ic_android);
            } else {
                drawable = ContextCompat.getDrawable(view.getContext(), R.drawable.ic_folder);
            }
            holder.information.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);

            if (selection != null) {
                if (selection[i]) {
                    holder.information.setBackgroundColor(Color.LTGRAY);
                } else {
                    holder.information.setBackgroundColor(Color.WHITE);
                }
            }
            return view;
        }

        static class ViewHolder {
            TextView information;

            ViewHolder(TextView information) {
                this.information = information;
            }
        }
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored", "ConstantConditions"})
    private void deleteFileOrFolder(File fileOrFolder) {
        if (fileOrFolder.isDirectory()) {
            if (fileOrFolder.list().length == 0) {
                fileOrFolder.delete();
            } else {
                String[] files = fileOrFolder.list();
                for (String temp : files) {
                    File fileToDelete = new File(fileOrFolder, temp);
                    deleteFileOrFolder(fileToDelete);
                }
                if (fileOrFolder.list().length == 0) {
                    fileOrFolder.delete();
                }
            }
        } else {
            fileOrFolder.delete();
        }
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public void onRequestPermissionsResult(final int requestCode, final String[] permissions, final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS && grantResults.length > 0) {
            if (arePermissionsDenied()) {
                ((ActivityManager) Objects.requireNonNull(this.getSystemService(ACTIVITY_SERVICE))).clearApplicationUserData();
                recreate();
            } else {
                onResume();
            }
        }
    }

    private boolean arePermissionsDenied() {
        int p = 0;
        while (p < PERMISSIONS_COUNT) {
            if (checkSelfPermission(PERMISSIONS[p]) != PackageManager.PERMISSION_GRANTED) {
                return true;
            }
            p++;
        }
        return false;
    }

}