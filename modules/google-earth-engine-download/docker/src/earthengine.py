import logging
from threading import Thread

import time
from ee.batch import Task

logger = logging.getLogger(__name__)


class EarthEngineStatus(object):
    def __init__(self, task_id, listener):
        self.task_id = task_id
        self.listener = listener
        self.running = True
        self.thread = Thread(
            name='GEE_Export-' + task_id,
            target=self._poll_status)
        self.thread.start()

    def cancel(self):
        self.stop()
        task = self._task()
        if task:
            logging.debug('Cancelling Google Earth Engine export task: ' + self.task_id)
            task.cancel()

    def stop(self):
        if self.running:
            logging.debug('Stopping EarthEngineStatus')
            self.running = False

    def _poll_status(self):
        logging.debug('Starting polling status of Google Earth Engine export task: ' + self.task_id)
        try:
            while self.running:
                status = self._gee_status()
                self.listener.update_status(status)
                if status['state'] != 'ACTIVE':
                    self.stop()
                    return
                time.sleep(10)
        except Exception:
            logger.exception('Export to Google Drive failed. Task id: ' + self.task_id)
            self.listener.update_status({
                'state': 'FAILED',
                'description': 'Export to Google Drive failed'})
            self.stop()

    def _task(self):
        tasks = Task.list()
        matching_tasks = list(task for task in tasks if task.id == self.task_id)
        return matching_tasks[0] if matching_tasks else None

    def _gee_status(self):
        task = self._task()
        if task:
            return self._to_status(task.status())
        else:
            return {'state': 'FAILED',
                    'description': 'Export to Google Drive failed'}

    def _to_status(self, gee_status):
        state = gee_status['state']
        if state == Task.State.FAILED:
            return {'state': 'FAILED',
                    'description': state['error_message']}
        elif state in [Task.State.CANCEL_REQUESTED, Task.State.CANCELLED]:
            return {'state': 'CANCELED',
                    'description': 'Download cancelled'}
        elif state == Task.State.COMPLETED:
            return {'state': 'ACTIVE',
                    'description': 'Downloading from Google Drive',
                    'step': 'EXPORTED',
                    'path': gee_status['description'] + '.tif'}
        else:
            return {'state': 'ACTIVE',
                    'description': 'Exporting to Google Drive',
                    'step': 'EXPORTING'}
