ALTER TABLE listening_test
    ADD COLUMN IF NOT EXISTS allow_audio_seek TINYINT DEFAULT 0 AFTER allow_pause;
