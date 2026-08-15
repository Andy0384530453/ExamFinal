insert into transcript (id, student_id, promotion_id, status, pdf_url, email, generated_at)
values
    ('aaaaaaaa-0000-0000-0000-000000000001', '22222222-0000-0000-0000-000000000015', null, 'PENDING', null, 'hei.andy.100@gmail.com', null),
    ('aaaaaaaa-0000-0000-0000-000000000002', '22222222-0000-0000-0000-000000000015', '11111111-0000-0000-0000-000000000001', 'GENERATED', 'https://dummy-bucket.s3.eu-west-3.amazonaws.com/transcripts/aaaaaaaa-0000-0000-0000-000000000002.pdf', 'hei.andy.100@gmail.com', '2026-08-14T09:00:00Z'),
    ('aaaaaaaa-0000-0000-0000-000000000003', '22222222-0000-0000-0000-000000000015', '11111111-0000-0000-0000-000000000002', 'EMAIL_SENT', 'https://dummy-bucket.s3.eu-west-3.amazonaws.com/transcripts/aaaaaaaa-0000-0000-0000-000000000003.pdf', 'hei.andy.100@gmail.com', '2026-08-14T10:00:00Z'),
    ('aaaaaaaa-0000-0000-0000-000000000004', '22222222-0000-0000-0000-000000000016', null, 'GENERATED', 'https://dummy-bucket.s3.eu-west-3.amazonaws.com/transcripts/aaaaaaaa-0000-0000-0000-000000000004.pdf', 'andrianasoloandy164@gmail.com', '2026-08-14T09:30:00Z'),
    ('aaaaaaaa-0000-0000-0000-000000000005', '22222222-0000-0000-0000-000000000016', '11111111-0000-0000-0000-000000000001', 'EMAIL_SENT', 'https://dummy-bucket.s3.eu-west-3.amazonaws.com/transcripts/aaaaaaaa-0000-0000-0000-000000000005.pdf', 'andrianasoloandy164@gmail.com', '2026-08-14T10:30:00Z'),
    ('aaaaaaaa-0000-0000-0000-000000000006', '22222222-0000-0000-0000-000000000016', '11111111-0000-0000-0000-000000000003', 'FAILED', null, 'andrianasoloandy164@gmail.com', '2026-08-14T11:00:00Z');
