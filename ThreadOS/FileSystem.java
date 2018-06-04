public class FileSystem
{
    private SuperBlock superBlock;
    private Directory directory;
    private FileStructureTable fileTable;

    private static final int SEEK_SET = 0;
    private static final int SEEK_CUR = 1;
    private static final int SEEK_END = 2;
    private static final int ERROR = -1;

    public FileSystem(int diskBlocks )
    {
        superBlock = new SuperBlock(diskBlocks);
        directory = new Directory(superBlock.totalInodes);
        fileTable = new FileStructureTable(directory);

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

    public void sync()
    {
        FileTableEntry rootEntry = open("/", "w");
        write(rootEntry, directory.directory2bytes());
        close(rootEntry);
        superBlock.sync();
    }

    public boolean format(int files)
    {
        if(files <= 0)
            return ERROR;
        while(!fileTable.fempty())
            ;
        superBlock.format(files);
        directory = new Directory(superBlock.totalInodes);
        fileTable = new FileStructureTable(directory);
        return true;
    }

    public FileTableEntry open(String fName, String mode)
    {
        if(fName == "" || mode == "")
            return null;
        FileTableEntry newFte = fileTable.falloc(fName, mode);
        if(newFte == null || newFte.inode.flag == 4)
        {
            fileTable.ffree(newFte);
            return null;
        }
        synchronized(newFte)
        {
            if(mode == "w" && !deallocAllBlocks(newFte))
            {
                fileTable.ffree(newFte);
                return null;
            }
        }
        return newFte;
    }

    public boolean deallocAllBlocks(FileTableEntry fte)
    {
        if(fte.inode.count == 0)
            return false;

        // returning direct disk blocks to free list
        for(short dirBlock = 0; dirBlock < fte.inode.directSize; dirBlock++)
        {
            if(fte.inode.direct[dirBlock] == -1)
                continue;
            superBlock.returnBlock(fte.inode.direct[dirBlock]);          // plz double chek
            fte.inode.direct[dirBlock] = -1;
        }

        byte[] data = fte.inode.freeIndirectBlock();
        if(data != null)
            if((int indirect = SysLib.bytes2short(data, 0)) != -1)
                superBlock.returnBlock(indirect);

        fte.inode.toDisk(fte.iNumber);
        return true;
    }

    publc int read(FileTableEntry fte, byte buf[])
    {
        if(fte == null || fte.mode = "w")
            return ERROR;

        int fteSize = fsize(fte);
        int bytesRead = 0;
        synchronized(fte)
        {
            while(bytesRead < buf.length && fte.seekPtr < fteSize)
            {
                if((int readBlock = fte.inode.findTargetBlock(fte.seekPtr)) == ERROR)
                    break;
                byte[] readBuf = new byte[Disk.blockSize];
                SysLib.rawread(readBlock, readBuf);

                int offset = fte.seekPtr % Disk.blockSize;
                int bytesLeft = buf.length - bytesRead;
                int blockBytesAvailable = Disk.blockSize - offset;
                int fteBytesAvailable = fteSize - fte.seekPtr;
                // WHY?
                int curReadSize = Math.min(bytesLeft, Math.min(blockBytesAvailable, fteBytesAvailable));

                System.arraycopy(readBuf, offset, buf, bytesRead, curReadSize);

                bytesRead += curReadSize;
                fte.seekPtr += curReadSize;
            }   
            return bytesRead;
        }
    }

    public int write(FileTableEntry fte, byte buf[])
    {
        if(fte == null || fte.mode = "r")
            return ERROR;

        int bytesWritten = 0;

        synchronized(fte)
        {
            while(bytesWritten < buf.length)
            {
                // find block number to be written
                int toBeWritten = fte.inode.findTargetBlock(fte.seekPtr);

                // if block was not found
                if(toBeWritten == ERROR)
                {
                    // find free block
                    short freeBlock = (short) superBlock.getFreeBlock();

                    // set an inode pointer to point to free block
                    switch(toBeWritten = fte.inode.setTargetBlock(fte.seekPtr, freeBlock))
                    {
                        case -1: return ERROR; // invalid access to used inode pointer
                        case -2: // invalid access to indirect pointer that is never used
                            short inBlock = (short) superBlock.getFreeBlock(); // find free block for indirect block
                            if(!fte.inode.setIndexBlock(freeBlock)) // format indirect block
                                return ERROR;
                            if((toBeWritten = fte.inode.setTargetBlock(fte.seekPtr, freeBlock)) == false) // add free block pointer to indirect pointer
                                return ERROR;
                            break;
                        default: // pointer to free block stored in inode pointer 0 - 265
                            break;
                    }
                }

                byte[] writeBuf = new byte[Disk.blockSize];
                SysLib.rawread(toBeWritten, buffer);

                short seekOffset = (short) (fte.seekPtr % Disk.blockSize);
                int bytesLeft = buf.length - bytesWritten;
                int bytesAvailable = Disk.blockSize - seekOffset;
                int writeBytes = Math.min(bytesLeft, bytesAvailable);

                //arraycopy(Object src, int srcPos, Object dest, int destPos, int length)
                System.arraycopy(buf, bytesWritten, writeBuf, seekOffset, writeBytes);
                SysLib.rawwrite(toBeWritten, writeBuf);

                bytesWritten += writeBytes;
                fte.seekPtr += writeBytes;
                if(fte.seekPtr > fte.inode.length)
                    fte.inode.length = fte.seekPtr;
            }
            fte.inode.toDisk(fte.iNumber);
            return bytesWritten;
        }
    }

    public int seek(FileTableEntry fte, int offset, int whence)
    {
        if(fte == null)
            return ERROR;
        synchronized(fte)
        {
            switch(whence)
            {
                case SEEK_SET:
                    if(offset < 0 || offset > fsize(fte))
                        return ERROR;
                    fte.seekPtr = offset;
                    break;
                case SEEK_CUR:
                    if(offset < 0 || offset > fsize(fte))
                        return ERROR;
                    fte.seekPtr += offset;
                    break;
                case SEEK_END:
                    fte.seekPtr = fsize(fte) + offset;
                    break;
                default:
                    break;
            }
        }
        return fte.seekPtr;
    }

    public boolean close(FileTableEntry fte)
    {
        synchronized(fte)
        {
            fte.count--;
            if(fte.count > 0)
                return true;
        }
        return fileTable.ffree(fte);
    }

    public boolean delete(String fName)
    {
        if((FileTableEntry fte = open(fName, "r")) == null)
            return false;
        short inode = fte.iNumber;
        if(close(fte))
            return directory.ifree(inode);
        return false;
    }

    public int fsize(FileTableEntry fte)
    {
        synchronized(fte)
        {
            return fte.inode.length;
        }
    }
}