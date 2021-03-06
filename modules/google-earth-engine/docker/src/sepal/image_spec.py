import logging
import uuid
from abc import abstractmethod

import ee

from download import export
from download.download import Downloader


class ImageSpec(object):
    def preview(self):
        """Creates Google Earth Engine mapId/token pair for rendering the image.

        :return: A dictionary with mapId and token
        :rtype: dict
        """
        viz_params = self._viz_params()
        ee_image = self._ee_image()
        logging.debug('Requesting map id of ' + str(self))
        ee_preview = ee_image.getMapId(viz_params)
        logging.debug('Got map id of ' + str(self) + ': ' + str(ee_preview))
        return {
            'mapId': ee_preview['mapid'],
            'token': ee_preview['token']
        }

    def download(self, name, username, credentials, downloader):
        """Starts to download the image in the background.

        :param name: The name to assign the downloaded file (excluding .tif)
        :type name: str

        :param username: The username of the user downloading.
        :type username: str

        :param credentials: The Google credenteials
        :type credentials: oauth2client.client.Credentials

        :param downloader: The download to use
        :type downloader: Downloader

        :return: The task id of the download
        :rtype: str
        """
        file_id = 'sepal-' + name + '-' + str(uuid.uuid4())
        task_id = export.to_drive(self._ee_image(), self.aoi.geometry().bounds(), name, username, file_id, self.scale)
        downloader.start_download(task_id, name, file_id, credentials)
        return task_id

    @abstractmethod
    def _ee_image(self):
        """Creates an ee.Image based on the spec.

        :return: An ee.Image
        :rtype: ee.Image
        """
        raise AssertionError('Method in subclass expected to have been invoked')

    @abstractmethod
    def _viz_params(self):
        """Returns the visualization parameters of this image.

        :return: The visualization parameters.
        :rtype: dict
        """
        raise AssertionError('Method in subclass expected to have been invoked')
