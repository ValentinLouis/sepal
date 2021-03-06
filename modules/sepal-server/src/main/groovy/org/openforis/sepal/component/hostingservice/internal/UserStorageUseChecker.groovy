package org.openforis.sepal.component.hostingservice.internal

import java.util.regex.Pattern

class UserStorageUseChecker {
    private final String userHomePathTemplate

    UserStorageUseChecker(String userHomePathTemplate) {
        this.userHomePathTemplate = userHomePathTemplate
    }

    double determineUsage(String username) {
        def userHome = new File(userHomePathTemplate.replaceAll(Pattern.quote('%user%'), username))
        if (!userHome.exists())
            throw new IllegalStateException("User home directory doesn't exist: $userHome")

        def storageUsedFile = new File(userHome, '.storageUsed')
        if (!storageUsedFile.exists())
            return 0
        def lines = storageUsedFile.readLines()
        if (!lines)
            return 0
        def size = lines.last().find('[0-9]*') as int
        return size / 1000 / 1000
    }
}
