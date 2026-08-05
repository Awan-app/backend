import { Injectable } from '@angular/core';

const DEVICE_ID_KEY = 'ezdo_device_id';

@Injectable({ providedIn: 'root' })
export class DeviceService {
  readonly deviceId: string = this.loadOrCreate();

  private loadOrCreate(): string {
    let id = localStorage.getItem(DEVICE_ID_KEY);
    if (!id) {
      id = crypto.randomUUID();
      localStorage.setItem(DEVICE_ID_KEY, id);
    }
    return id;
  }
}
