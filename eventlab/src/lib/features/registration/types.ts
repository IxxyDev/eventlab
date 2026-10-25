export type Registration = {
  id: string;
  name: string;
  email: string;
};

export type RegistrationCreatedResponse = {
  registration: Registration;
  accessToken: string;
};

export type RegistrationRequest = {
  name: string;
  email: string;
};
