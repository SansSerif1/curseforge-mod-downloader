import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class Main {
    public static File jarfolder = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile();
    public static File extractfolder;
    public static void main(String[] args) {
        //set colors
        String ANSI_RESET = "\u001B[0m";
        String ANSI_RED = "\u001B[31m";
        String ANSI_GREEN = "\u001B[32m";
        String ANSI_YELLOW = "\u001B[33m";
        String ANSI_CYAN = "\u001B[36m";
        String INTNEEDED = "\033[1;31m";
        if (args.length == 0) {
            System.out.println(INTNEEDED + "Please specify the extracted CurseForge ZIP path (the folder containing manifest.json, overrides, etc.)" + ANSI_RESET);
            System.out.println(INTNEEDED + "If you dont know how to specify the folder, do \"java -jar CFMDownloader.jar C:\\Path\\To\\Folder\"" + ANSI_RESET);
            System.exit(1);
        }
        extractfolder = new File(args[0]);
        if (!extractfolder.exists()) {
            System.out.println(INTNEEDED + "The specified folder doesnt exist. You must specify a valid path." + ANSI_RESET);
            System.exit(1);
        }
        if (!extractfolder.isDirectory()) {
            System.out.println(INTNEEDED + "The specified path is not a folder. Specify the extracted folder containing manifest.json, etc." + ANSI_RESET);
            System.exit(1);
        }
        File manifest = new File(extractfolder.getAbsolutePath() + File.separator + "manifest.json");
        if (!manifest.exists()) {
            System.out.println(INTNEEDED + "manifest.json doesnt exist in the specified folder. Please download a CurseForge (not MultiMC) version" + ANSI_RESET);
            System.exit(1);
        }

        String manifestext = null;
        try {
            manifestext = FileUtils.readFileToString(manifest, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.out.println(ANSI_RED + "IOException while reading manifest." + ANSI_RESET);
            System.exit(1);
        }
        JSONObject json = new JSONObject(manifestext);
        System.out.println(ANSI_CYAN + "LOG: Downloading Modpack: \"" + json.getString("name") + "\", Version: \"" + json.getString("version") + "\", by \"" + json.getString("author") + "\"." + ANSI_RESET);
        File modfolder = new File(jarfolder.getAbsolutePath() + File.separator + "Downloaded" + File.separator + json.getString("name").replace(" ", "_") + "_" + json.getString("version"));
        modfolder.mkdirs();

        JSONArray mods = json.getJSONArray("files");
        for (int i = 0; i != mods.length(); i++) {
            JSONObject modinfo = mods.getJSONObject(i);
            String modurl = null;
            try {
                modurl = IOUtils.toString(new URL("https://addons-ecs.forgesvc.net/api/v2/addon/" + modinfo.getInt("projectID") + "/file/" + modinfo.getInt("fileID") + "/download-url"), StandardCharsets.UTF_8);
            } catch (IOException e) {
                System.out.println(ANSI_RED + "Cant get direct mod download link." + ANSI_RESET);
                System.exit(1);
            }
            //save the mod
            File modfile = new File(modfolder.getAbsolutePath() + File.separator + FilenameUtils.getName(modurl));
            if (modfile.exists()) {
                System.out.println(ANSI_YELLOW + "WARNING: Mod \"" + FilenameUtils.getName(modurl) + "\" exists, skipping." + ANSI_RESET);
            } else {
                try {
                    FileUtils.copyURLToFile(new URL(modurl), modfile);
                } catch (Exception e) {
                    System.out.println(ANSI_RED + "Could not save downloaded mod." + ANSI_RESET);
                    System.exit(1);
                }
                System.out.println(ANSI_GREEN + "LOG: Sucessfully downloaded mod \"" + FilenameUtils.getName(modurl) + "\"" + ANSI_RESET);
            }
        }
        System.out.println(ANSI_GREEN + "LOG: All mods from manifest.json successfully downloaded." + ANSI_RESET);
        System.out.println(ANSI_CYAN + "LOG: Generating README.txt." + ANSI_RESET);
        File readme = new File(modfolder + File.separator + "README.txt");
        try {
            readme.delete();
            readme.createNewFile();
            FileWriter readmewriter = new FileWriter(readme);
            readmewriter.write("--- " + json.getString("name") + " v. " + json.getString("version") + " by " + json.getString("author") + " ---\n");
            JSONObject packinfo = json.getJSONObject("minecraft");
            readmewriter.write("Made for: Minecraft " + packinfo.getString("version") + "\n");
            readmewriter.write("Loaders:\n");
            for (int i = 0; i != packinfo.getJSONArray("modLoaders").length(); i++) {
                String primary = "";
                if (packinfo.getJSONArray("modLoaders").getJSONObject(i).getBoolean("primary")) primary = "(Primary)";
                readmewriter.write("   - " + packinfo.getJSONArray("modLoaders").getJSONObject(i).getString("id") + " " + primary + "\n");
            }
            readmewriter.write("\n\n\n");
            readmewriter.write("You can download modloaders here:\n");
            readmewriter.write("   - Minecraft Forge - https://files.minecraftforge.net/net/minecraftforge/forge/\n");
            readmewriter.write("   - Fabric - https://fabricmc.net/use/\n");
            readmewriter.write("\n");
            readmewriter.write("Dont forget to copy stuff from overrides folder (from the extracted modpack folder) to your installation folder if you want to have the same mod configurations as in the downloaded modpack :)");
            readmewriter.close();
        } catch (IOException e) {
            System.out.println(ANSI_RED + "Could not create the README.txt file." + ANSI_RESET);
            System.exit(1);
        }
        System.out.println(ANSI_CYAN + "LOG: Successfully downloaded all mod files and created a README.txt file. Dont forget to read it, its in the same folder as the mods (folder with this .jar file / Downloads / Modpack /)" + ANSI_RESET);
    }
}
