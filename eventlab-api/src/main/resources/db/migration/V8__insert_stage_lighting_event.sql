INSERT INTO organizations (id, name)
VALUES ('luch-lab', 'Лаборатория света «Луч»');

INSERT INTO events (
  id,
  organization_id,
  title,
  description,
  starts_at,
  ends_at,
  time_zone,
  venue,
  capacity
)
VALUES (
  'stage-lighting',
  'luch-lab',
  'Свет на сцене: от пульта до прожектора',
  'Соберём световую партитуру для короткого номера и разберём, как пульт управляет приборами.',
  '2026-11-07 19:00 Asia/Yekaterinburg',
  '2026-11-07 21:30 Asia/Yekaterinburg',
  'Asia/Yekaterinburg',
  'Дом культуры «Октябрь», большой зал',
  20
);
