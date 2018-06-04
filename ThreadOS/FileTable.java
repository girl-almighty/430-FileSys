import java.util.Vector;
public class FileTable {

    private Vector<FileTableEntry> table;         // the actual entity of this file table
    private Directory dir;        // the root directory

    public FileTable( Directory directory ) { // constructor
        table = new Vector<FileTableEntry>( );     // instantiate a file (structure) table
        dir = directory;           // receive a reference to the Director
    }                             // from the file system

    // major public methods
    public synchronized FileTableEntry falloc( String filename, String mode ) {
        // allocate a new file (structure) table entry for this file name
        // allocate/retrieve and register the corresponding inode using dir
        // increment this inode's count
        // immediately write back this inode to the disk
        // return a reference to this file (structure) table entry

        short iNum = -1;
        Inode inode = null;

        while (true){

            // since directory consider as a file, if it is a root, then set to inode 0
            if (filename.equals("/")) {
                iNum = 0;
            } else {
                iNum = this.dir.namei(filename);
            }

            if (iNum < 0){
                SysLib.cout("iNumber can smaller than 0, back to fileTable.java (falloc function)");
            }

            if(iNum >= 0) {
                inode = new Inode(iNum);

                if (inode.flag == 2) { // delete mode
                    return null;
                }
                if (inode.flag == 0 || inode.flag == 1) {
                    break;
                }
                if (mode.equals(0) && inode.flag == 0) {
                    break;
                }
                try {
                    wait();
                }
                catch (InterruptedException e) {
                }
            }
        }
        // allocate a new file table entry for this file name
        FileTableEntry newEntry = new FileTableEntry(inode,iNum,mode);
        // add new entry to filetable
        table.add(newEntry);
        // increment this inode's count
        inode.count++;
        // write back inode to the disk
        inode.toDisk(iNum);
        return newEntry;
    }

    public synchronized boolean ffree( FileTableEntry e ) {
        // receive a file table entry reference
        // save the corresponding inode to the disk
        // free this file table entry.
        // return true if this file table entry found in my table
        int entry = -1;
        // find entry index
        for(int i = 0; i <table.size(); i++){
            if(table.elementAt(i).equals(e))
                entry = i;
        }

        // if cant find entry, return false
        if (entry == -1)
            return false;

        // decrease threads use entry
        e.inode.count--;
        if (e.inode.count == 0)
            e.inode.flag = 0;
        e.inode.toDisk(e.iNumber);		                //save updated to disk
        return table.remove(table.elementAt(entry));   //remove e from table
    }


    public synchronized boolean fempty( ) {
        return table.isEmpty( );  // return if table is empty
    }                            // should be called before starting a format
}
