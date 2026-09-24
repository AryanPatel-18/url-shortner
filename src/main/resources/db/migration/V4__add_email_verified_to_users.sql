-- Default to TRUE so existing users remain verified and do not get locked out
ALTER TABLE users ADD COLUMN email_verified BOOLEAN DEFAULT TRUE NOT NULL;