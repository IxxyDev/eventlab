export type EventDetails = {
  id: string;
  title: string;
  description: string;
  startsAt: string;
  endsAt: string;
  timeZone: string;
  venue: string;
  capacity: number;
  availableSeats: number;
  registrationOpen: boolean;
};
