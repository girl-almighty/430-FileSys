public class FileSystem
{
    private SuperBlock superblock;
    private Directory directory;
    private FileStructureTable filetable;

    public FileSystem(int diskBlocks )
    {
        superblock = new SuperBlock(diskBlocks);
        directory = new Directory(superblock.totalInodes);
        filetable = new FileStructureTable(directory);

        FileTableEntry dirEnt = open ("/", "r");
        int dirSize = fsize(dirEnt);
        if(dirSize > 0)
        {
            byte[] dirData = new byte[dirSize];
            read(dirEnt, dirData);
            directory.bytes2directory(dirdata);
        }
        close(dirEnt);
    }

    public int format(int files)
    {
        
    }

    public int open(String fileName, String mode)
    {
        return 0;
    }

    publc int read(int fd, byte buf[])
    {
    }

    public int write(int fd, byte buf[])
    {
    }

    public int seek(int fd, int offset, int whence)
    {
    }

    public int close(int fd)
    {
    }

    public int delete(String fileName)
    {
    }

    public int fsize(int fd)
    {

    }
}