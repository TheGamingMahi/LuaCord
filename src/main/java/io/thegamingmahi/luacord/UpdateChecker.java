package io.thegamingmahi.luacord;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;

import java.util.ArrayList;

/**
 * Lukkit update checking class.
 *
 * @author jammehcow
 */
public class UpdateChecker {
    // If the repo ever gets moved again this should make it easier.
    private static final String GITHUB_ORG = "artex-development";
    /**
     * Check for updates against the GitHub repo releases page.
     *
     * @param pluginVersion the locally installed plugin version
     */
    public static void checkForUpdates(String pluginVersion) {
        try {
            HttpResponse<JsonNode> res = Unirest.get("https://api.github.com/repos/" + GITHUB_ORG + "/Lukkit/releases/latest").asJson();
            String tagName = res.getBody().getObject().getString("tag_name").replace("v", "");

            if (isOutOfDate(pluginVersion.split("-")[0], tagName)) {
                Main.logger.info("A new version of Lukkit has been released: " + tagName);
                Main.logger.info("You can download it from https://www.spigotmc.org/resources/lukkit.32599/ or https://github.com/jammehcow/Lukkit/releases");
            } else {
                Main.logger.info("You're up to date with the latest version of Lukkit.");
            }
        } catch (Exception e ) { e.printStackTrace(); }
    }

    /**
     * Checks if the local version is out of date by comparing the current version with the remote.
     *
     * @param current the currently installed version
     * @param remote  the remote version (GitHub releases)
     * @return whether the local version is lower than the remote version.
     */
    public static boolean isOutOfDate(String current, String remote) {
        // Version are formatted as "x.x.x" so we split on every period and convert to ints
        ArrayList<Integer> currentVersion = new ArrayList<>(3);
        ArrayList<Integer> remoteVersion = new ArrayList<>(3);

        // Formats the ArrayList to look like {1, 2, 3}
        currentVersion.addAll(getIntegers(current.split("\\.")));
        remoteVersion.addAll(getIntegers(remote.split("\\.")));

        for (int i = 0; i < currentVersion.size(); i++) {
            if (currentVersion.get(i).compareTo(remoteVersion.get(i)) < 0) return true;
            else if (currentVersion.get(i).compareTo(remoteVersion.get(i)) > 0) return false;
        }

        return false;
    }

    private static ArrayList<Integer> getIntegers(String[] numbers) {
        ArrayList<Integer> ints = new ArrayList<>();
        for (String number : numbers) ints.add(Integer.parseInt(number));

        return ints;
    }
}
