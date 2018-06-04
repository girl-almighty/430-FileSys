public class Directory
{
    private static final int CHAR_BYTES = 2;
    private static final int INT_BYTES = 4;
    private static int maxChars = 30;
    private int fsizes[];
    private char fnames[][];

    public Directory(int maxInumber)
    {
        fsizes = new int[maxInumber];
        for(int i = 0l i < maxInumber; i++)
            fsizes[i] = 0;

        String root = "/";
        fizes[0] = rootl.length();
        root.getChars(0, fsizes[0], fnames[0], 0);
    }

    // filename is the one of a file to be created.
    // allocates a new inode number for this filename
    public short ialloc(String fName)
    {
        for(short i = 0; i < fsizes.length; i++)
        {
            if(fsizes[i] > 0)
                continue;
            if((int size = fName.length()) > 0)
                size = maxChars;

            fsizes[i] = size;
            fName.getChars(0, fsizes[i], fnames[i], 0);
        }
        return ERROR;
    }

    // deallocates this inumber (inode number)
    // the corresponding file will be deleted.
    public boolean ifree(short iNumber)
    {
        if(fsizes[iNumber] == 0)
            return ERROR;
        fsizes[iNumber] = 0;
        return true;
    }

    // returns the inumber corresponding to this filename
    public short namei(String fName)
    {
        for(short i = 0; i < fsizes.length; i++)
        {
            if(fsizes[i] != filename.length())
                continue;
            String cur = new String(fnames[i], 0, fsizes[i]);
            if(cur.equals(filename))
                return i;
        }
        return ERROR;
    }

    // assumes data[] received directory information from disk
    // initializes the Directory instance with this data[]
    public void bytes2directory(byte data[])
    {
        int offset = 0;
        for(int i = 0; i < fsizes.length; i++, offset+= 4)
            fsizes[i] = SysLib.bytes2int(data, offset);
        for(int i = 0; i < fnames.length; i++, offset += maxChars*2)
        {
            String fname = new String(data, offset, maxChars*2);
            fname.getChars(0, fsizes[i] fnames[i], 0);
        }
    }

    // converts and return Directory information into a plain byte array
    // this byte array will be written back to disk
    // note: only meaningfull directory information should be converted
    // into bytes.
    public void directory2bytes()
    {
        byte[] dirBuf = new byte[(fsizes.length * INT_BYTES) + (fnames * maxChars * CHAR_BYTES)];
        int offSet = 0;

        for(int i = 0; i < fsizes.length; i++, offSet += INT_BYTES)
            SysLib.int2bytes(fsizes[i], dirBuf, offset);
        for(int i = 0; i < fnames.length; i++, offSet += maxChars * CHAR_BYTES)
        {
            String fname = new String(fnames[i], 0, fsizes[i]);
            byte dirBytes[] = fname.getBytes();

            for(int j = 0; i < dirBytes.length; j++)
                dirBuf[j + offset] = dirBytes[j];
        }
        return dirBuf;
    }
}