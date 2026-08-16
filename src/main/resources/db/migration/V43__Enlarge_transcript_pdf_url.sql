do
$$
begin
    if exists (select 1
               from information_schema.tables
               where table_schema = 'public'
                 and table_name = 'transcript') then
        alter table transcript
            alter column pdf_url type text;
    end if;
end
$$;
