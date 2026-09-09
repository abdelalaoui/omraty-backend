package com.omraty.backend.storage;

final class FileExtensions {

    private FileExtensions() {}

    static String of(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        int dotIndex = originalFilename.lastIndexOf('.');
        return dotIndex >= 0 ? originalFilename.substring(dotIndex) : "";
    }
}
