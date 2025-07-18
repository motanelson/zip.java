import java.io.*;
import java.util.*;
import java.util.zip.CRC32;
import java.nio.file.Files;
import java.nio.file.Path;
public class zip {
    static void writeU16(OutputStream out, int value) throws IOException {
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
    }

    static void writeU32(OutputStream out, long value) throws IOException {
        out.write((int) (value & 0xFF));
        out.write((int) ((value >> 8) & 0xFF));
        out.write((int) ((value >> 16) & 0xFF));
        out.write((int) ((value >> 24) & 0xFF));
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.print("\033c\033[43;30m\nFicheiros a incluir (separados por espaço): ");
        String line = sc.nextLine();
        String[] files = line.trim().split("\\s+");
        String zipname = "output.zip";

        try (FileOutputStream zip = new FileOutputStream(zipname)) {
            List<byte[]> names = new ArrayList<>();
            List<Long> crcs = new ArrayList<>();
            List<Long> sizes = new ArrayList<>();
            List<Long> offsets = new ArrayList<>();
            ByteArrayOutputStream centralDir = new ByteArrayOutputStream();

            for (String filename : files) {
                File f = new File(filename);
                if (!f.exists()) {
                    System.out.println("Ignorado (não existe): " + filename);
                    continue;
                }

                byte[] data = Files.readAllBytes(f.toPath());
                byte[] nameBytes = filename.getBytes("UTF-8");
                long crc = calcCRC32(data);
                long offset = zip.getChannel().position();

                // Local File Header
                zip.write(new byte[]{'P', 'K', 3, 4});
                writeU16(zip, 20); // version needed
                writeU16(zip, 0);  // flags
                writeU16(zip, 0);  // method = store
                writeU16(zip, 0);  // time
                writeU16(zip, 0);  // date
                writeU32(zip, crc); // CRC
                writeU32(zip, data.length); // compressed size
                writeU32(zip, data.length); // uncompressed size
                writeU16(zip, nameBytes.length); // file name len
                writeU16(zip, 0); // extra len
                zip.write(nameBytes); // file name
                zip.write(data); // file data

                // Save for central dir
                names.add(nameBytes);
                crcs.add(crc);
                sizes.add((long) data.length);
                offsets.add(offset);
            }

            long cdStart = zip.getChannel().position();

            // Write Central Directory
            for (int i = 0; i < names.size(); i++) {
                byte[] name = names.get(i);
                long crc = crcs.get(i);
                long size = sizes.get(i);
                long offset = offsets.get(i);

                centralDir.write(new byte[]{'P', 'K', 1, 2});
                writeU16(centralDir, 0x0314); // version made by (DOS + 20)
                writeU16(centralDir, 20); // version needed
                writeU16(centralDir, 0); // flags
                writeU16(centralDir, 0); // method
                writeU16(centralDir, 0); // time
                writeU16(centralDir, 0); // date
                writeU32(centralDir, crc);
                writeU32(centralDir, size);
                writeU32(centralDir, size);
                writeU16(centralDir, name.length);
                writeU16(centralDir, 0); // extra
                writeU16(centralDir, 0); // comment
                writeU16(centralDir, 0); // disk #
                writeU16(centralDir, 0); // internal attr
                writeU32(centralDir, 0); // external attr
                writeU32(centralDir, offset);
                centralDir.write(name);
            }

            byte[] cdData = centralDir.toByteArray();
            zip.write(cdData);

            // End of central directory
            zip.write(new byte[]{'P', 'K', 5, 6});
            writeU16(zip, 0); // this disk
            writeU16(zip, 0); // disk with CD
            writeU16(zip, names.size()); // total entries on this disk
            writeU16(zip, names.size()); // total entries total
            writeU32(zip, cdData.length); // CD size
            writeU32(zip, cdStart); // CD offset
            writeU16(zip, 0); // comment length

            System.out.println("ZIP criado: " + zipname);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    static long calcCRC32(byte[] data) {
        CRC32 crc = new CRC32();
        crc.update(data);
        return crc.getValue();
    }
}
