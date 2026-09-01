CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ==========================================================
-- ENUM TYPES
-- ==========================================================
CREATE TYPE user_role AS ENUM ('USER', 'MODERATOR', 'ADMIN');
CREATE TYPE user_status AS ENUM ('ACTIVE', 'RESTRICTED', 'BLOCKED');
CREATE TYPE book_source AS ENUM ('GOOGLE_BOOKS', 'MANUAL');
CREATE TYPE delivery_method AS ENUM ('PICKUP', 'MAIL');
CREATE TYPE listing_status AS ENUM ('AVAILABLE', 'RESERVED', 'IN_EXCHANGE', 'ARCHIVED');
CREATE TYPE request_status AS ENUM ('PENDING', 'ACTIVE', 'REJECTED', 'CANCELLED');
CREATE TYPE exchange_status AS ENUM ('HANDOVER_PENDING', 'IN_READING', 'RETURN_PENDING', 'COMPLETED', 'OVERDUE', 'DISPUTED');
CREATE TYPE photo_stage AS ENUM ('HANDOVER', 'RETURN');
CREATE TYPE shipment_direction AS ENUM ('TO_READER', 'TO_OWNER');
CREATE TYPE shipment_status AS ENUM ('PENDING', 'SHIPPED', 'DELIVERED');
CREATE TYPE shipment_carrier AS ENUM ('NOVA_POSHTA', 'UKRPOSHTA', 'OTHER');
CREATE TYPE extension_status AS ENUM ('PENDING', 'APPROVED', 'REJECTED');
CREATE TYPE dispute_status AS ENUM ('OPEN', 'RESOLVED_FAVOR_FILER', 'RESOLVED_FAVOR_OTHER', 'DISMISSED');
CREATE TYPE restriction_type AS ENUM ('TEMPORARY', 'PERMANENT');

-- ==========================================================
-- USERS
-- ==========================================================
CREATE TABLE users (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       google_id VARCHAR(64) UNIQUE,
                       email VARCHAR(255) UNIQUE NOT NULL,
                       password_hash VARCHAR(255),
                       name VARCHAR(255) NOT NULL,
                       avatar_url VARCHAR(512),
                       city VARCHAR(255),
                       bio TEXT,
                       rating_avg NUMERIC(3,2) DEFAULT 0,
                       books_taken INT DEFAULT 0,
                       books_returned_on_time INT DEFAULT 0,
                       books_overdue INT DEFAULT 0,
                       books_damaged INT DEFAULT 0,
                       books_given INT DEFAULT 0,
                       role user_role DEFAULT 'USER',
                       status user_status DEFAULT 'ACTIVE',
                       restricted_until TIMESTAMP,
                       created_at TIMESTAMP DEFAULT now(),
                       updated_at TIMESTAMP DEFAULT now()
);

-- ==========================================================
-- BOOK CATALOG (довідкові дані про книгу)
-- ==========================================================
CREATE TABLE book_catalog_entries (
                                      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                      isbn VARCHAR(20) UNIQUE,
                                      title VARCHAR(500) NOT NULL,
                                      author VARCHAR(500),
                                      description TEXT,
                                      genre VARCHAR(255),
                                      cover_url VARCHAR(512),
                                      source book_source DEFAULT 'MANUAL',
                                      external_id VARCHAR(100),
                                      created_at TIMESTAMP DEFAULT now()
);

-- ==========================================================
-- LISTINGS (оголошення про конкретний примірник)
-- ==========================================================
CREATE TABLE listings (
                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                          book_catalog_entry_id UUID NOT NULL REFERENCES book_catalog_entries(id),
                          condition_description TEXT,
                          delivery_methods TEXT[] NOT NULL DEFAULT '{}'
                              CHECK (delivery_methods <@ ARRAY['PICKUP','MAIL']::text[]),
                          status listing_status DEFAULT 'AVAILABLE',
                          created_at TIMESTAMP DEFAULT now(),
                          updated_at TIMESTAMP DEFAULT now()
);
CREATE INDEX idx_listings_owner ON listings(owner_id);
CREATE INDEX idx_listings_status ON listings(status);

CREATE TABLE listing_photos (
                                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                listing_id UUID NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
                                url VARCHAR(512) NOT NULL,
                                created_at TIMESTAMP DEFAULT now()
);

CREATE TABLE wishlist_items (
                                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                book_catalog_entry_id UUID REFERENCES book_catalog_entries(id),
                                query_title VARCHAR(500),
                                created_at TIMESTAMP DEFAULT now()
);

-- ==========================================================
-- REQUESTS (черга заявок)
-- ==========================================================
CREATE TABLE requests (
                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          listing_id UUID NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
                          requester_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                          desired_deadline DATE NOT NULL,
                          preferred_delivery_method delivery_method NOT NULL,
                          message TEXT,
                          status request_status DEFAULT 'PENDING',
                          reject_comment TEXT,
                          created_at TIMESTAMP DEFAULT now(),
                          decided_at TIMESTAMP
);
CREATE INDEX idx_requests_listing ON requests(listing_id);
CREATE INDEX idx_requests_requester ON requests(requester_id);

-- ==========================================================
-- EXCHANGES
-- ==========================================================
CREATE TABLE exchanges (
                           id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                           request_id UUID NOT NULL UNIQUE REFERENCES requests(id),
                           listing_id UUID NOT NULL REFERENCES listings(id),
                           owner_id UUID NOT NULL REFERENCES users(id),
                           reader_id UUID NOT NULL REFERENCES users(id),
                           delivery_method delivery_method NOT NULL,
                           deadline DATE NOT NULL,
                           extended_deadline DATE,
                           status exchange_status DEFAULT 'HANDOVER_PENDING',
                           created_at TIMESTAMP DEFAULT now(),
                           completed_at TIMESTAMP
);
CREATE INDEX idx_exchanges_owner ON exchanges(owner_id);
CREATE INDEX idx_exchanges_reader ON exchanges(reader_id);
CREATE INDEX idx_exchanges_status ON exchanges(status);

CREATE TABLE exchange_photos (
                                 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                 exchange_id UUID NOT NULL REFERENCES exchanges(id) ON DELETE CASCADE,
                                 uploaded_by UUID NOT NULL REFERENCES users(id),
                                 stage photo_stage NOT NULL,
                                 url VARCHAR(512) NOT NULL,
                                 note TEXT,
                                 created_at TIMESTAMP DEFAULT now()
);

CREATE TABLE shipment_info (
                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               exchange_id UUID NOT NULL REFERENCES exchanges(id) ON DELETE CASCADE,
                               direction shipment_direction NOT NULL,
                               recipient_name VARCHAR(255) NOT NULL,
                               recipient_phone VARCHAR(32) NOT NULL,
                               carrier shipment_carrier NOT NULL,
                               city VARCHAR(255) NOT NULL,
                               branch_number VARCHAR(20) NOT NULL,
                               waybill_photo_url VARCHAR(512),
                               status shipment_status NOT NULL DEFAULT 'PENDING',
                               shipped_at TIMESTAMP,
                               delivered_at TIMESTAMP
);

CREATE TABLE deadline_extension_requests (
                                             id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                             exchange_id UUID NOT NULL REFERENCES exchanges(id) ON DELETE CASCADE,
                                             requested_new_deadline DATE NOT NULL,
                                             status extension_status DEFAULT 'PENDING',
                                             comment TEXT,
                                             created_at TIMESTAMP DEFAULT now(),
                                             decided_at TIMESTAMP
);

-- ==========================================================
-- REVIEWS
-- ==========================================================
CREATE TABLE reviews (
                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                         exchange_id UUID NOT NULL REFERENCES exchanges(id) ON DELETE CASCADE,
                         author_id UUID NOT NULL REFERENCES users(id),
                         target_id UUID NOT NULL REFERENCES users(id),
                         rating SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
                         comment TEXT,
                         created_at TIMESTAMP DEFAULT now()
);
CREATE INDEX idx_reviews_target ON reviews(target_id);

-- ==========================================================
-- DISPUTES & RESTRICTIONS
-- ==========================================================
CREATE TABLE disputes (
                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          exchange_id UUID NOT NULL REFERENCES exchanges(id) ON DELETE CASCADE,
                          filed_by UUID NOT NULL REFERENCES users(id),
                          description TEXT NOT NULL,
                          status dispute_status DEFAULT 'OPEN',
                          moderator_id UUID REFERENCES users(id),
                          resolution_comment TEXT,
                          created_at TIMESTAMP DEFAULT now(),
                          resolved_at TIMESTAMP
);

CREATE TABLE restrictions (
                              id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                              user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                              type restriction_type NOT NULL,
                              reason TEXT,
                              dispute_id UUID REFERENCES disputes(id),
                              starts_at TIMESTAMP DEFAULT now(),
                              ends_at TIMESTAMP
);

-- ==========================================================
-- CHAT
-- ==========================================================
CREATE TABLE chat_rooms (
                            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                            exchange_id UUID UNIQUE REFERENCES exchanges(id) ON DELETE CASCADE,
                            user_a_id UUID NOT NULL REFERENCES users(id),
                            user_b_id UUID NOT NULL REFERENCES users(id),
                            created_at TIMESTAMP DEFAULT now()
);

CREATE TABLE chat_messages (
                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               chat_room_id UUID NOT NULL REFERENCES chat_rooms(id) ON DELETE CASCADE,
                               sender_id UUID NOT NULL REFERENCES users(id),
                               content TEXT NOT NULL,
                               created_at TIMESTAMP DEFAULT now(),
                               read_at TIMESTAMP
);
CREATE INDEX idx_chat_messages_room ON chat_messages(chat_room_id);

-- ==========================================================
-- FEED
-- ==========================================================
CREATE TABLE posts (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       author_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                       content TEXT NOT NULL,
                       photo_url VARCHAR(512),
                       created_at TIMESTAMP DEFAULT now()
);

CREATE TABLE comments (
                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          post_id UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
                          author_id UUID NOT NULL REFERENCES users(id),
                          content TEXT NOT NULL,
                          created_at TIMESTAMP DEFAULT now()
);

-- ==========================================================
-- NOTIFICATIONS
-- ==========================================================
CREATE TABLE notifications (
                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                               type VARCHAR(50) NOT NULL,
                               reference_id UUID,
                               message VARCHAR(500) NOT NULL,
                               is_read BOOLEAN DEFAULT false,
                               created_at TIMESTAMP DEFAULT now()
);
CREATE INDEX idx_notifications_user ON notifications(user_id, is_read);