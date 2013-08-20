
package a2copy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.webcodepro.applecommander.storage.DirectoryEntry;
import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.DiskFullException;
import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FormattedDisk;
import com.webcodepro.applecommander.storage.os.prodos.ProdosFileEntry;

/*
 * This class uses AppleCommander's command-line interface to extract an entire
 * set of files and directories from an image, or build a whole image from
 * files and directories.
 * 
 * Has specific hard-coded addresses for Lawless Legends files; should fix this
 * at some point to use a metadata configuration file or something like that.
 */
public class A2Copy
{
  /*
   * Main command-line driver
   */
  public static void main(String[] args)
    throws IOException, DiskFullException 
  {
    try
    {
      if (args[0].equals("-get")) {
        getDirFromImg(args[1], args[2], args[3]);
        return;
      }
      if (args[0].equals("-put")) {
        putDirToImg(args[1], args[2], args[3]);
        return;
      }
    }
    catch (ArrayIndexOutOfBoundsException e)
    { }
    System.err.format("Usage: A2copy [-get imgFile srcImgDir destLocalDir] | [-put imgFile dstImgDir srcLocalDir]\n");
    System.err.format("       where srcImgDir/dstImgDir is a subdirectory in the image, or / for the root directory.\n");
    System.exit(1);
  }
  
  /** Patterns used for parsing filenames and paths */
  static Pattern extPat = Pattern.compile("^(.*)\\.([^.]+)$");
  static Pattern hashPat = Pattern.compile("^(.*)#([^#]+$)");
  static Pattern pathPat = Pattern.compile("^/?([^/]+)(.*$)");
  
  /**
   * Extract all the files and subdirectories from one directory in an image file, and
   * write them to the local filesystem.
   * 
   * @throws IOException        if something goes wrong
   * @throws DiskFullException  this actually can't happen (because we're not creating dirs)
   */
  static void getDirFromImg(String imgFile, String srcImgDir, String dstLocalDir) throws IOException, DiskFullException
  {
    // Create the local dir if necessary.
    File localDirFile = new File(dstLocalDir);
    localDirFile.mkdirs();
    
    // Open the image file and get the disk inside it.
    FormattedDisk fd = new Disk(imgFile).getFormattedDisks()[0];

    // Locate the right subdirectory on the disk.
    DirectoryEntry imgDir = findSubDir(fd, srcImgDir, false);
    
    // Recursively extract the files from that subdirectory.
    getAllFiles((List<FileEntry>)imgDir.getFiles(), localDirFile);
  }
  
  static DirectoryEntry findSubDir(DirectoryEntry imgDir, String subDirs, boolean create) 
    throws DiskFullException
  {
    Matcher m = pathPat.matcher(subDirs);
    if (m.matches())
    {
      // Process next component of the directory path.
      String subName = m.group(1);
      String remaining = m.group(2);
      for (FileEntry e : (List<FileEntry>)imgDir.getFiles()) {
        if (!e.isDeleted() && e.isDirectory() && e.getFilename().equalsIgnoreCase(subName))
          return findSubDir((DirectoryEntry)e, remaining, create);
      }

      // Not found. If we're not allowed to create it, error out.
      if (!create) {
        System.err.format("Error: subdirectory '%s' not found.\n", subDirs);
        System.exit(2);
      }

      // Create the subdirectory and continue to sub-sub-dirs.
      return findSubDir(imgDir.createDirectory(subName.toUpperCase()), remaining, create);
    }
    
    return imgDir;
  }

  /**
   * Helper for file/directory extraction.
   * @param files               set of files to extract
   * @param dstTargetDir        where to put them
   * @throws IOException        if something goes wrong
   */
  static void getAllFiles(List<FileEntry> files, File dstTargetDir) throws IOException
  {
    // Ensure the target directory exists
    dstTargetDir.mkdir();
    
    // Make a map of the existing filesystem files so we can match them. This way,
    // we can retain whatever case regime the user has established on the filesystem.
    //
    HashMap<String, File> existingFiles = new HashMap<>();
    HashMap<String, String> baseMap = new HashMap<>();
    for (File f: dstTargetDir.listFiles()) 
    {
      if (!f.isFile())
        continue;
      String name = f.getName();
      existingFiles.put(name.toLowerCase(), f);

      Matcher m = hashPat.matcher(name);
      if (m.matches())
        name = m.group(1);
      existingFiles.put(name.toLowerCase(), f);

      m = extPat.matcher(name);
      if (m.matches())
        name = m.group(1);
      existingFiles.put(name.toLowerCase(), f);
      
      baseMap.put(f.getName(), name);
    }
    
    // Process each entry in the list
    for (FileEntry e : files)
    {
      // Skip deleted things
      if (e.isDeleted())
        continue;
      
      // Determine the filename we should use locally. If there's a matching
      // file already here, use its base.
      //
      String baseName = e.getFilename().toLowerCase();
      if (existingFiles.containsKey(baseName)) {
        File existingFile = existingFiles.get(baseName);
        baseName = baseMap.get(existingFile.getName());
        existingFile.delete();
      }
      
      // Recursively process sub-directories
      if (e.isDirectory()) {
        File subDir = new File(dstTargetDir, baseName);
        getAllFiles(((DirectoryEntry)e).getFiles(), subDir);
        continue;
      }
      
      // Add a hash for the file type.
      String outName = baseName + "." + e.getFiletype().toLowerCase();
        
      // Add a hash for the address if this kind of entry uses one.
      if (e.needsAddress()) {
        int auxType = ((ProdosFileEntry)e).getAuxiliaryType();
        outName = outName + "#" + Integer.toHexString(auxType);
      }
      
      // Ready to copy the data.
      byte[] data = e.getFileData();
      try (FileOutputStream out = new FileOutputStream(new File(dstTargetDir, outName))) {
        out.write(data);
      }
    }
  }
  
  /**
   * Put a whole directory of files/subdirs from the local filesystem into a 
   * subdirectory of an image file.
   * 
   * @param imgFilePath         path to the image file
   * @param dstImgDir           subdirectory in the image file, or "/" for the root
   * @param srcLocalDir         directory containing files and subdirs
   * @throws DiskFullException  if the image file fills up
   * @throws IOException        if something else goes wrong
   */
  static void putDirToImg(String imgFilePath, String dstImgDir, String srcLocalDir)
    throws IOException, DiskFullException 
  {
    // Make sure the local dir exists.
    File localDirFile = new File(srcLocalDir);
    if (!localDirFile.isDirectory()) {
      System.err.format("Error: Local directory '%s' not found.\n", srcLocalDir);
      System.exit(2);
    }
    
    // Open the image file.
    FormattedDisk fd = new Disk(imgFilePath).getFormattedDisks()[0];
    
    // Get to the right sub-directory.
    DirectoryEntry ent = findSubDir(fd, dstImgDir, true);
    
    // And fill it up.
    putAllFiles(fd, ent, localDirFile);
  }

  /**
   * Helper for image creation.
   * 
   * @param fd                  disk to insert files into
   * @param targetDir           directory within the disk
   * @param srcDir              filesystem directory to read
   * @throws DiskFullException  if the image file fills up
   * @throws IOException        if something else goes wrong
   */
  private static void putAllFiles(FormattedDisk fd, DirectoryEntry targetDir, File srcDir)
    throws DiskFullException, IOException 
  {
    // Process each file in the source directory
    for (File srcFile : srcDir.listFiles())
    {
      if (srcFile.isDirectory()) {
        DirectoryEntry subDir = targetDir.createDirectory(srcFile.getName().toUpperCase());
        putAllFiles(fd, subDir, srcFile);
        continue;
      }
      
      // Parse and strip the hash (address) and extension if any
      String name = srcFile.getName();
      String hash = "0";
      Matcher m = hashPat.matcher(name);
      if (m.matches()) {
        name = m.group(1);
        hash = m.group(2);
      }
      
      String ext = "0";
      m = extPat.matcher(name);
      if (m.matches()) {
        name = m.group(1);
        ext = m.group(2);
      }
      
      // Create a new entry in the disk image for this file.
      FileEntry ent = targetDir.createFile();
      ent.setFilename(name.toUpperCase());
      
      // Set the file type using the extension we parsed above.
      ent.setFiletype(ext);
      
      // Set the address if we have one and this kind of file wants one.
      if (ent.needsAddress())
      {
        try {
          ent.setAddress(Integer.parseInt(hash, 16));
        }
        catch (NumberFormatException e) { /*pass*/ }
      }
      
      // Copy the file data
      FileInputStream in = new FileInputStream(srcFile);
      byte[] buf = new byte[(int) srcFile.length()];
      int nRead = in.read(buf);
      if (nRead != srcFile.length())
        throw new IOException(String.format("Error reading file '%s'", srcFile.toString()));
      ent.setFileData(buf);
      
      // And save the new entry.
      fd.save();
    }
  }
}
