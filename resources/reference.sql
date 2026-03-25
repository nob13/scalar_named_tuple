CREATE TABLE "user" (
    id INT PRIMARY KEY,
    email TEXT NOT NULL,
    age INT,
    name TEXT,
    address_street TEXT,
    address_zip TEXT,
    address_city TEXT
);

CREATE TABLE "permission" (
    id INT PRIMARY KEY,
    name TEXT 
);

CREATE TABLE "user_permission" (
    user_id INT REFERENCES "user" (id),
    permission_id INT REFERENCES "permission" (permission_id),
    PRIMARY KEY(user_id, permission_id)
);
