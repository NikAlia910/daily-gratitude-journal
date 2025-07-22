import dayjs from 'dayjs';
import { IUser } from 'app/shared/model/user.model';
import { Mood } from 'app/shared/model/enumerations/mood.model';

export interface IGratitudeEntry {
  id?: number;
  date?: dayjs.Dayjs;
  entry?: string;
  mood?: keyof typeof Mood | null;
  timestamp?: dayjs.Dayjs;
  user?: IUser | null;
}

export const defaultValue: Readonly<IGratitudeEntry> = {};
