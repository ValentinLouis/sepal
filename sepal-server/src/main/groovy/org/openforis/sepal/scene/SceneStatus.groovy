package org.openforis.sepal.scene


enum SceneStatus {
    UNKNOWN(-1L), REQUESTED(0L), INVALID(1), STARTED(2L), DOWNLOADING(3L), DOWNLOADED(4L), TRANSFORMING(5L), TRANSFORMED(6L), PROCESSING(7L), PROCESSED(8L), PUBLISHING(9L), PUBLISHED(10L), FAILED(11L)

    final Long requestCode

    SceneStatus(Long requestCode) {
        this.requestCode = requestCode
    }

    def static byValue(final String requestStatus) {
        SceneStatus status = UNKNOWN
        for (SceneStatus value : values()) {
            if (value.toString().equals(requestStatus)) {
                status = value
                break
            }
        }
        return status
    }
}