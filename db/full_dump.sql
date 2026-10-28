--
-- PostgreSQL database dump
--

\restrict U7vsfofliswd1xotb66Lwqe1tKfoe3PPMdEGgTDlyM5m84iirkBaN3ZRZKWjPul

-- Dumped from database version 16.11
-- Dumped by pg_dump version 16.11

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: pgcrypto; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;


--
-- Name: EXTENSION pgcrypto; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON EXTENSION pgcrypto IS 'cryptographic functions';


--
-- Name: book_source; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.book_source AS ENUM (
    'GOOGLE_BOOKS',
    'MANUAL'
);


--
-- Name: delivery_method; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.delivery_method AS ENUM (
    'PICKUP',
    'MAIL'
);


--
-- Name: dispute_status; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.dispute_status AS ENUM (
    'OPEN',
    'RESOLVED_FAVOR_FILER',
    'RESOLVED_FAVOR_OTHER',
    'DISMISSED'
);


--
-- Name: exchange_status; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.exchange_status AS ENUM (
    'HANDOVER_PENDING',
    'IN_READING',
    'RETURN_PENDING',
    'COMPLETED',
    'OVERDUE',
    'DISPUTED'
);


--
-- Name: extension_status; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.extension_status AS ENUM (
    'PENDING',
    'APPROVED',
    'REJECTED'
);


--
-- Name: listing_status; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.listing_status AS ENUM (
    'AVAILABLE',
    'RESERVED',
    'IN_EXCHANGE',
    'ARCHIVED'
);


--
-- Name: photo_stage; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.photo_stage AS ENUM (
    'HANDOVER',
    'RETURN',
    'DISPUTE'
);


--
-- Name: request_status; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.request_status AS ENUM (
    'PENDING',
    'ACTIVE',
    'REJECTED',
    'CANCELLED'
);


--
-- Name: restriction_type; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.restriction_type AS ENUM (
    'TEMPORARY',
    'PERMANENT'
);


--
-- Name: settlement_type; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.settlement_type AS ENUM (
    'CITY',
    'VILLAGE',
    'SETTLEMENT'
);


--
-- Name: shipment_carrier; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.shipment_carrier AS ENUM (
    'NOVA_POSHTA',
    'UKRPOSHTA',
    'OTHER'
);


--
-- Name: shipment_direction; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.shipment_direction AS ENUM (
    'TO_READER',
    'TO_OWNER'
);


--
-- Name: shipment_status; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.shipment_status AS ENUM (
    'PENDING',
    'SHIPPED',
    'DELIVERED'
);


--
-- Name: user_role; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.user_role AS ENUM (
    'USER',
    'MODERATOR',
    'ADMIN'
);


--
-- Name: user_status; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.user_status AS ENUM (
    'ACTIVE',
    'RESTRICTED',
    'BLOCKED'
);


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: book_catalog_entries; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.book_catalog_entries (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    isbn character varying(20),
    title character varying(500) NOT NULL,
    author character varying(500),
    description text,
    genre character varying(255),
    cover_url character varying(512),
    source public.book_source DEFAULT 'MANUAL'::public.book_source,
    external_id character varying(100),
    created_at timestamp without time zone DEFAULT now()
);


--
-- Name: chat_messages; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.chat_messages (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    chat_room_id uuid NOT NULL,
    sender_id uuid NOT NULL,
    content text NOT NULL,
    created_at timestamp without time zone DEFAULT now(),
    read_at timestamp without time zone
);


--
-- Name: chat_rooms; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.chat_rooms (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_a_id uuid NOT NULL,
    user_b_id uuid NOT NULL,
    created_at timestamp without time zone DEFAULT now()
);


--
-- Name: comments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.comments (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    post_id uuid NOT NULL,
    author_id uuid NOT NULL,
    content text NOT NULL,
    created_at timestamp without time zone DEFAULT now()
);


--
-- Name: deadline_extension_requests; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.deadline_extension_requests (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    exchange_id uuid NOT NULL,
    requested_new_deadline date,
    status public.extension_status DEFAULT 'PENDING'::public.extension_status,
    comment text,
    created_at timestamp without time zone DEFAULT now(),
    decided_at timestamp without time zone,
    decision_comment text,
    extra_days integer NOT NULL,
    CONSTRAINT chk_extension_extra_days CHECK (((extra_days >= 1) AND (extra_days <= 60)))
);


--
-- Name: deadline_reminders; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.deadline_reminders (
    exchange_id uuid NOT NULL,
    kind character varying(20) NOT NULL,
    due_date date NOT NULL,
    sent_at timestamp without time zone DEFAULT now() NOT NULL,
    CONSTRAINT deadline_reminders_kind_check CHECK (((kind)::text = ANY ((ARRAY['SOON'::character varying, 'LAST_DAY'::character varying, 'OVERDUE'::character varying])::text[])))
);


--
-- Name: disputes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.disputes (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    exchange_id uuid NOT NULL,
    filed_by uuid NOT NULL,
    description text NOT NULL,
    status public.dispute_status DEFAULT 'OPEN'::public.dispute_status,
    moderator_id uuid,
    resolution_comment text,
    created_at timestamp without time zone DEFAULT now(),
    resolved_at timestamp without time zone
);


--
-- Name: exchange_photos; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.exchange_photos (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    exchange_id uuid NOT NULL,
    uploaded_by uuid NOT NULL,
    stage public.photo_stage NOT NULL,
    url character varying(512) NOT NULL,
    note text,
    created_at timestamp without time zone DEFAULT now()
);


--
-- Name: exchanges; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.exchanges (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    request_id uuid NOT NULL,
    listing_id uuid NOT NULL,
    owner_id uuid NOT NULL,
    reader_id uuid NOT NULL,
    delivery_method public.delivery_method NOT NULL,
    deadline date,
    extended_deadline date,
    status public.exchange_status DEFAULT 'HANDOVER_PENDING'::public.exchange_status,
    dispute_reason text,
    created_at timestamp without time zone DEFAULT now(),
    completed_at timestamp without time zone,
    reading_days integer NOT NULL,
    reading_started_at timestamp without time zone,
    return_prepared_at timestamp without time zone,
    returned_at timestamp without time zone,
    reading_paused_days integer DEFAULT 0 NOT NULL
);


--
-- Name: feedback; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.feedback (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    topic character varying(20) NOT NULL,
    email character varying(255) NOT NULL,
    message text NOT NULL,
    link character varying(512),
    screenshot_url character varying(512),
    user_id uuid,
    ip_address character varying(64),
    status character varying(20) DEFAULT 'NEW'::character varying NOT NULL,
    moderator_note text,
    handled_by uuid,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    CONSTRAINT chk_feedback_status CHECK (((status)::text = ANY ((ARRAY['NEW'::character varying, 'IN_PROGRESS'::character varying, 'RESOLVED'::character varying])::text[]))),
    CONSTRAINT chk_feedback_topic CHECK (((topic)::text = ANY ((ARRAY['QUESTION'::character varying, 'BUG'::character varying, 'IDEA'::character varying, 'COMPLAINT'::character varying])::text[])))
);


--
-- Name: listing_photos; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.listing_photos (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    listing_id uuid NOT NULL,
    url character varying(512) NOT NULL,
    created_at timestamp without time zone DEFAULT now()
);


--
-- Name: listings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.listings (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    owner_id uuid NOT NULL,
    book_catalog_entry_id uuid NOT NULL,
    condition_description text,
    settlement_type public.settlement_type,
    region character varying(255),
    settlement_name character varying(255),
    delivery_methods text[] DEFAULT '{}'::text[] NOT NULL,
    status public.listing_status DEFAULT 'AVAILABLE'::public.listing_status,
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now(),
    hidden_at timestamp without time zone,
    hidden_reason text,
    hidden_by uuid,
    CONSTRAINT listings_delivery_methods_check CHECK ((delivery_methods <@ ARRAY['PICKUP'::text, 'MAIL'::text]))
);


--
-- Name: moderation_actions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.moderation_actions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    moderator_id uuid,
    action character varying(40) NOT NULL,
    target_type character varying(20) NOT NULL,
    target_id uuid,
    target_user_id uuid,
    reason text,
    details text,
    created_at timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: notifications; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.notifications (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    type character varying(50) NOT NULL,
    reference_id uuid,
    message character varying(500) NOT NULL,
    is_read boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: post_likes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.post_likes (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    post_id uuid NOT NULL,
    user_id uuid NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: posts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.posts (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    author_id uuid NOT NULL,
    content text NOT NULL,
    photo_url character varying(512),
    created_at timestamp without time zone DEFAULT now()
);


--
-- Name: requests; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.requests (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    listing_id uuid NOT NULL,
    requester_id uuid NOT NULL,
    preferred_delivery_method public.delivery_method NOT NULL,
    message text,
    status public.request_status DEFAULT 'PENDING'::public.request_status,
    reject_comment text,
    created_at timestamp without time zone DEFAULT now(),
    decided_at timestamp without time zone,
    reading_days integer NOT NULL,
    CONSTRAINT chk_requests_reading_days CHECK (((reading_days >= 1) AND (reading_days <= 90)))
);


--
-- Name: restrictions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.restrictions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    type public.restriction_type NOT NULL,
    reason text,
    dispute_id uuid,
    starts_at timestamp without time zone DEFAULT now(),
    ends_at timestamp without time zone,
    created_by uuid,
    lifted_at timestamp without time zone,
    lifted_by uuid
);


--
-- Name: reviews; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.reviews (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    exchange_id uuid NOT NULL,
    author_id uuid NOT NULL,
    target_id uuid NOT NULL,
    rating smallint NOT NULL,
    comment text,
    created_at timestamp without time zone DEFAULT now(),
    CONSTRAINT reviews_rating_check CHECK (((rating >= 1) AND (rating <= 5)))
);


--
-- Name: shipment_info; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.shipment_info (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    exchange_id uuid NOT NULL,
    direction public.shipment_direction NOT NULL,
    recipient_name character varying(255) NOT NULL,
    recipient_phone character varying(32) NOT NULL,
    carrier public.shipment_carrier NOT NULL,
    city character varying(255) NOT NULL,
    branch_number character varying(20) NOT NULL,
    waybill_photo_url character varying(512),
    status public.shipment_status DEFAULT 'PENDING'::public.shipment_status NOT NULL,
    shipped_at timestamp without time zone,
    delivered_at timestamp without time zone
);


--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    google_id character varying(64),
    email character varying(255) NOT NULL,
    password_hash character varying(255),
    name character varying(255) NOT NULL,
    avatar_url character varying(512),
    settlement_type public.settlement_type,
    region character varying(255),
    settlement_name character varying(255),
    bio text,
    rating_avg numeric(3,2) DEFAULT 0,
    books_taken integer DEFAULT 0,
    books_returned_on_time integer DEFAULT 0,
    books_overdue integer DEFAULT 0,
    books_damaged integer DEFAULT 0,
    books_given integer DEFAULT 0,
    role public.user_role DEFAULT 'USER'::public.user_role,
    status public.user_status DEFAULT 'ACTIVE'::public.user_status,
    restricted_until timestamp without time zone,
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now()
);


--
-- Name: wishlist_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.wishlist_items (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    book_catalog_entry_id uuid,
    query_title character varying(500),
    created_at timestamp without time zone DEFAULT now()
);


--
-- Data for Name: book_catalog_entries; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.book_catalog_entries (id, isbn, title, author, description, genre, cover_url, source, external_id, created_at) FROM stdin;
a1111111-0000-0000-0000-000000000001	9789661376165	Кобзар	Тарас Шевченко	Збірка поетичних творів.	Поезія	https://upload.wikimedia.org/wikipedia/uk/3/3e/%D0%9E%D0%BA%D0%BB%D0%B0%D0%B4%D0%B8%D0%BD%D0%BA%D0%B0_%D0%9A%D0%BE%D0%B1%D0%B7%D0%B0%D1%80%D1%8F_%D0%B2%D0%B8%D0%B4%D0%B0%D0%BD%D0%BE%D0%B3%D0%BE_2002_%D1%80%D0%BE%D0%BA%D1%83.jpeg?utm_source=uk.wikipedia.org&utm_campaign=imageinfo&utm_content=thumbnail_unscaled	GOOGLE_BOOKS	\N	2026-04-18 05:43:14.040917
a1111111-0000-0000-0000-000000000002	9786171253689	Затьмарення	Філіп К. Дік	Науково-фантастичний роман.	Фантастика	https://covers.openlibrary.org/b/isbn/9780156767293-L.jpg?default=false	GOOGLE_BOOKS	\N	2026-04-28 05:43:14.040917
a1111111-0000-0000-0000-000000000003	9786176791312	Хроніки Амбера	Роджер Желязни	Фентезійна сага.	Фентезі	https://covers.openlibrary.org/b/isbn/9780380014405-L.jpg?default=false	GOOGLE_BOOKS	\N	2026-05-18 05:43:14.040917
a1111111-0000-0000-0000-000000000004	9786177544996	Атомні звички	Джеймс Клір	Книга про формування корисних звичок.	Нон-фікшн	https://covers.openlibrary.org/b/isbn/9780735211292-L.jpg?default=false	GOOGLE_BOOKS	\N	2026-06-27 05:43:14.040917
a1111111-0000-0000-0000-000000000005	9786178012345	Місто	Валер'ян Підмогильний	Класика української прози.	Класика	https://vivat.com.ua/storage/1.d/files/f/c/fc61305b_9786178693015.jpg	MANUAL	\N	2026-07-17 05:43:14.040917
da717bbc-9ed8-42fb-96b9-2f4f3415c619	9780451524935	1984	Джордж Орвелл	\N	Антиутопія	https://covers.openlibrary.org/b/isbn/9780451524935-L.jpg?default=false	MANUAL	\N	2026-10-05 05:43:14.67561
9256f772-9822-4940-982c-c74fc6a00ab1	9780060850524	Дивний новий світ	Олдос Гакслі	\N	Антиутопія	https://covers.openlibrary.org/b/isbn/9780060850524-L.jpg?default=false	MANUAL	\N	2026-10-05 05:43:14.67561
b5c5b2e0-56a9-46ba-ad59-b8b0bd4d4c8f	9780061120084	Убити пересмішника	Харпер Лі	\N	Класика	https://covers.openlibrary.org/b/isbn/9780061120084-L.jpg?default=false	MANUAL	\N	2026-10-05 05:43:14.67561
496a751c-2f5d-43ea-b44a-c8df7c9f6efa	9780743273565	Великий Гетсбі	Френсіс Скотт Фіцджеральд	\N	Класика	https://covers.openlibrary.org/b/isbn/9780743273565-L.jpg?default=false	MANUAL	\N	2026-10-05 05:43:14.67561
f2d52c59-26e1-482f-887d-c50fce53adc8	9780141439747	Олівер Твіст	Чарльз Діккенс	\N	Класика	https://covers.openlibrary.org/b/isbn/9780141439747-L.jpg?default=false	MANUAL	\N	2026-10-05 05:43:14.67561
66789058-6fa0-4ae1-8861-8ccf2055fbd4	9780141441146	Джейн Ейр	Шарлотта Бронте	\N	Класика	https://covers.openlibrary.org/b/isbn/9780141441146-L.jpg?default=false	MANUAL	\N	2026-10-05 05:43:14.67561
ec10989f-c393-4909-9dc8-1694234cd286	9780141439556	Грозовий Перевал	Емілі Бронте	\N	Класика	https://covers.openlibrary.org/b/isbn/9780141439556-L.jpg?default=false	MANUAL	\N	2026-10-05 05:43:14.67561
1b670d23-6146-4517-871a-e755d24c8bc8	9780441013593	Дюна	Френк Герберт	\N	Наукова фантастика	https://covers.openlibrary.org/b/isbn/9780441013593-L.jpg?default=false	MANUAL	\N	2026-10-05 05:43:14.67561
9537744f-baea-4de8-9e93-c1c46d9c72a0	9780553103540	Гра престолів	Джордж Мартін	\N	Фентезі	https://covers.openlibrary.org/b/isbn/9780553103540-L.jpg?default=false	MANUAL	\N	2026-10-05 05:43:14.67561
13c74ae5-5062-4750-9a34-4f696be53a74	9780618640157	Володар перснів: Братство персня	Джон Толкін	\N	Фентезі	https://covers.openlibrary.org/b/isbn/9780618640157-L.jpg?default=false	MANUAL	\N	2026-10-05 05:43:14.67561
1ed309d0-23c8-414e-b0e1-aab71c56a482	9780747532699	Гаррі Поттер і філософський камінь	Джоан Роулінг	\N	Фентезі	https://covers.openlibrary.org/b/isbn/9780747532699-L.jpg?default=false	MANUAL	\N	2026-10-05 05:43:14.67561
80d9cb61-ce54-4db9-97b4-d64e3703ea94	9780064404990	Хроніки Нарнії: Лев, Біла Відьма та шафа	Клайв Льюїс	\N	Фентезі	https://covers.openlibrary.org/b/isbn/9780064404990-L.jpg?default=false	MANUAL	\N	2026-10-05 05:43:14.67561
c6ae0362-6860-42da-a181-1b8128cbdbd6	9780451529260	Три мушкетери	Александр Дюма	\N	Пригоди	https://covers.openlibrary.org/b/isbn/9780451529260-L.jpg?default=false	MANUAL	\N	2026-10-05 05:43:14.67561
21052470-8496-45cb-90da-dc6f262ef9c4	9780140449266	Граф Монте-Крісто	Александр Дюма	\N	Пригоди	https://covers.openlibrary.org/b/isbn/9780140449266-L.jpg?default=false	MANUAL	\N	2026-10-05 05:43:14.67561
97a743f8-ddd5-4138-8453-1d87f710b6db	9780141439570	Портрет Доріана Грея	Оскар Вайлд	\N	Класика	https://covers.openlibrary.org/b/isbn/9780141439570-L.jpg?default=false	MANUAL	\N	2026-10-05 05:43:14.67561
762068c0-2ce8-4010-a206-5ef25193e699	9780142437247	Мобі Дік	Герман Мелвілл	\N	Класика	https://covers.openlibrary.org/b/isbn/9780142437247-L.jpg?default=false	MANUAL	\N	2026-10-05 05:43:14.67561
273a23e1-42f1-499d-9a3f-4c6f60df6b00	9780141439518	Гордість і упередження	Джейн Остін	\N	Класика	https://covers.openlibrary.org/b/isbn/9780141439518-L.jpg?default=false	MANUAL	\N	2026-10-05 05:43:14.67561
9238eb19-f568-44c5-84ab-251660f95da8	9780553212419	Пригоди Шерлока Холмса	Артур Конан Дойл	\N	Детектив	https://covers.openlibrary.org/b/isbn/9780553212419-L.jpg?default=false	MANUAL	\N	2026-10-05 05:43:14.67561
a564feff-6a38-47c2-9193-0ae3c3d2537d	9780062073495	Вбивство у Східному експресі	Агата Крісті	\N	Детектив	https://covers.openlibrary.org/b/isbn/9780062073495-L.jpg?default=false	MANUAL	\N	2026-10-05 05:43:14.67561
ec2fe2e3-9ec2-4dc8-81a9-c5b4f2dd5894	9780062073488	Десять негренят	Агата Крісті	\N	Детектив	https://covers.openlibrary.org/b/isbn/9780062073488-L.jpg?default=false	MANUAL	\N	2026-10-05 05:43:14.67561
282352bf-e7c0-4a34-a9fa-a99565c848fa	9780060934347	Дон Кіхот	Мігель де Сервантес	\N	Класика	https://covers.openlibrary.org/b/isbn/9780060934347-L.jpg?default=false	MANUAL	\N	2026-10-05 05:43:14.67561
901edc2b-5004-4f56-90f5-5e014c1f5e92	9780451419439	Знедолені	Віктор Гюго	\N	Класика	https://covers.openlibrary.org/b/isbn/9780451419439-L.jpg?default=false	MANUAL	\N	2026-10-05 05:43:14.67561
21001890-523d-4768-b6be-07dbc727630a	9780140443530	Собор Паризької Богоматері	Віктор Гюго	\N	Класика	https://covers.openlibrary.org/b/isbn/9780140443530-L.jpg?default=false	MANUAL	\N	2026-10-05 05:43:14.67561
196e8b9c-d397-40f9-b5ac-25411475b20c	9780141182816	Портрет митця замолоду	Джеймс Джойс	\N	Класика	https://covers.openlibrary.org/b/isbn/9780141182816-L.jpg?default=false	MANUAL	\N	2026-10-05 05:43:14.67561
629788dd-291b-4040-b458-11dac8d0504d	9780684801223	Старий і море	Ернест Гемінгвей	\N	Класика	https://covers.openlibrary.org/b/isbn/9780684801223-L.jpg?default=false	MANUAL	\N	2026-10-05 05:43:14.67561
4d1b7a1b-1c2b-4203-ad56-0c4660a2ffb0	9780684801469	Прощавай, зброє	Ернест Гемінгвей	\N	Класика	https://covers.openlibrary.org/b/isbn/9780684801469-L.jpg?default=false	MANUAL	\N	2026-10-05 05:43:14.67561
1adc869a-0678-4f1e-93ee-8f0ecc0190d3	9789660376389	Особливості національного винокуріння	Юрій Андрухович	\N	Проза	https://covers.openlibrary.org/b/isbn/9789660376389-L.jpg?default=false	MANUAL	\N	2026-10-05 05:43:14.67561
e201b2d8-0368-4753-a923-50612047e8cd	9789660396301	Польові дослідження з українського сексу	Оксана Забужко	\N	Проза	https://covers.openlibrary.org/b/isbn/9789660396301-L.jpg?default=false	MANUAL	\N	2026-10-05 05:43:14.67561
e9c1153c-2dca-4332-84be-02e6acb7c626	9789661452123	Танго смерті	Юрій Винничук	\N	Проза	https://covers.openlibrary.org/b/isbn/9789661452123-L.jpg?default=false	MANUAL	\N	2026-10-05 05:43:14.67561
86caa903-2f60-45fa-bac2-db32c6e46311	9789661421471	Століття Якова	Володимир Лис	\N	Проза	https://covers.openlibrary.org/b/isbn/9789661421471-L.jpg?default=false	MANUAL	\N	2026-10-05 05:43:14.67561
a1ae3063-07df-41c1-b895-42039175a64c	9781451673319	451 градус за Фаренгейтом	Рей Бредбері	\N	Наукова фантастика	https://covers.openlibrary.org/b/isbn/9781451673319-L.jpg?default=false	MANUAL	\N	2026-10-05 05:43:14.67561
85866955-e1cb-41c4-b40b-c6efaae584a6	9781451678192	Марсіанські хроніки	Рей Бредбері	\N	Наукова фантастика	https://covers.openlibrary.org/b/isbn/9781451678192-L.jpg?default=false	MANUAL	\N	2026-10-05 05:43:14.67561
edca2a98-9944-40a8-9377-bbe21d46726c	9780553283686	Гіперіон	Ден Сіммонс	\N	Наукова фантастика	https://covers.openlibrary.org/b/isbn/9780553283686-L.jpg?default=false	MANUAL	\N	2026-10-05 05:43:14.67561
58db7795-dc1b-4746-bce9-223e36be87e5	9780553293357	Основа	Айзек Азімов	\N	Наукова фантастика	https://covers.openlibrary.org/b/isbn/9780553293357-L.jpg?default=false	MANUAL	\N	2026-10-05 05:43:14.67561
ff6887fd-cd06-4858-886d-2ca4a17df394	9780345347954	Кінець дитинства	Артур Кларк	\N	Наукова фантастика	https://covers.openlibrary.org/b/isbn/9780345347954-L.jpg?default=false	MANUAL	\N	2026-10-05 05:43:14.67561
fd95c0a9-b640-427a-9d4d-0ab35734429e	9780812550702	Гра Ендера	Орсон Скотт Кард	\N	Наукова фантастика	https://covers.openlibrary.org/b/isbn/9780812550702-L.jpg?default=false	MANUAL	\N	2026-10-05 05:43:14.67561
2ecadb18-2770-4714-96c5-63d7e7484a0c	9780441569595	Нейромант	Вільям Гібсон	\N	Наукова фантастика	https://covers.openlibrary.org/b/isbn/9780441569595-L.jpg?default=false	MANUAL	\N	2026-10-05 05:43:14.67561
203ba202-c3db-42f8-9fe5-23209f7fc579	9780451528064	Бен Гур	Лью Воллес	\N	Пригоди	https://covers.openlibrary.org/b/isbn/9780451528064-L.jpg?default=false	MANUAL	\N	2026-10-05 05:43:14.67561
91c56c21-ac9d-4fca-aa59-af25f2e8e545	9780141321004	Острів скарбів	Роберт Луїс Стівенсон	\N	Пригоди	https://covers.openlibrary.org/b/isbn/9780141321004-L.jpg?default=false	MANUAL	\N	2026-10-05 05:43:14.67561
aa8cadcf-4612-4d52-ab55-7da5fba850d8	9780141439822	Робінзон Крузо	Даніель Дефо	\N	Пригоди	https://covers.openlibrary.org/b/isbn/9780141439822-L.jpg?default=false	MANUAL	\N	2026-10-05 05:43:14.67561
f804e166-5298-4e68-b517-f674fffe74f1	9780486400778	Пригоди Тома Сойєра	Марк Твен	\N	Пригоди	https://covers.openlibrary.org/b/isbn/9780486400778-L.jpg?default=false	MANUAL	\N	2026-10-05 05:43:14.67561
ea28ebbb-f5df-4d45-862b-3646f8f6c4f9	9780156012195	Маленький принц	Антуан де Сент-Екзюпері	\N	Проза	https://covers.openlibrary.org/b/isbn/9780156012195-L.jpg?default=false	MANUAL	\N	2026-10-05 05:43:14.67561
58c1d763-647a-4af3-b2fd-f42c240641f0	9780312278670	Степовий вовк	Герман Гессе	\N	Проза	https://covers.openlibrary.org/b/isbn/9780312278670-L.jpg?default=false	MANUAL	\N	2026-10-05 05:43:14.67561
70c03713-a762-4ecb-8bfc-9c01e3e187bc	9780805059167	Гра в бісер	Герман Гессе	\N	Проза	https://covers.openlibrary.org/b/isbn/9780805059167-L.jpg?default=false	MANUAL	\N	2026-10-05 05:43:14.67561
6c36b942-61bf-4030-b5e0-fd66c99c82e6	9780060883287	Сто років самотності	Габріель Гарсіа Маркес	\N	Класика	https://covers.openlibrary.org/b/isbn/9780060883287-L.jpg?default=false	MANUAL	\N	2026-10-05 05:43:14.67561
61a4d625-7995-4298-a5f2-8661cc5d941c	9781400034684	Кохання під час холери	Габріель Гарсіа Маркес	\N	Класика	https://covers.openlibrary.org/b/isbn/9781400034684-L.jpg?default=false	MANUAL	\N	2026-10-05 05:43:14.67561
950f4959-8d21-404a-924f-d08c83ff8d69	9780375704024	Норвезький ліс	Харукі Муракамі	\N	Проза	https://covers.openlibrary.org/b/isbn/9780375704024-L.jpg?default=false	MANUAL	\N	2026-10-05 05:43:14.67561
908d066b-5332-417e-ac48-b9800924eeb2	9781400079278	Кафка на пляжі	Харукі Муракамі	\N	Проза	https://covers.openlibrary.org/b/isbn/9781400079278-L.jpg?default=false	MANUAL	\N	2026-10-05 05:43:14.67561
fc55274d-8aa6-44da-8983-2b055ef75491	9780307743657	Сяйво	Стівен Кінг	\N	Жахи	https://covers.openlibrary.org/b/isbn/9780307743657-L.jpg?default=false	MANUAL	\N	2026-10-05 05:43:14.67561
93d0c33e-031a-487c-8622-73155aa4cbe2	9781501142970	Воно	Стівен Кінг	\N	Жахи	https://covers.openlibrary.org/b/isbn/9781501142970-L.jpg?default=false	MANUAL	\N	2026-10-05 05:43:14.67561
61230c0f-8e42-4489-bd7a-84c3909c1a44	9789669488237	Атомні звички. Легкий і перевірений спосіб набути корисних звичок і позбутися звичок шкідливих	Джеймс Клір	Бестселер "The New York Times"! Незначні зміни, значні результати! Дізнайтеся секрет тривалих результатів! Нашпигована заснованими на фактах стратегіями із самовдосконалення, книга "Атомні звички" навчить, як набути манер і навиків, що працюватимуть на вас, а не проти вас. У книзі Джеймс Клір ділиться надзвичайно цікавою і по-справжньому продуктивною методикою напрацювання корисних звичок. В її основі лежить поняття чотириступеневої моделі звичок (подразник, прагнення, відгук, винагорода), а також чотири породжені цією моделлю закони зміни поведінки. Викладений матеріал підтверджується результатами наукових досліджень, проте книгу слід трактувати не як наукову розвідку, а як посібник з експлуатації, що містить мудрі практичні поради та пояснення механізмів створення і змінювання власних звичок. Єдиного правильного способу набуття корисних звичок, звісно, не існує, але автор пропонує найкращий із відомих підхід, який спрацює незалежно від того, з чого саме ви починаєте і що саме намагаєтеся змінити. Викладені стратегії стануть у пригоді всім, хто потребує покрокової інструкції з покращення незалежно від того, що мається на меті - покращення здоров'я чи добробуту, продуктивності праці чи стосунків, а можливо, всього переліченого разом. Джеймс Клір- автор і лектор, який велику увагу приділяє таким питанням, як людські звички, механізм ухвалення рішень і методи постійного самовдосконалення. Його доробки друкувались у The New York Times, Entrepreneur, Time, а також були представлені глядачі американської ранкової телепрограми This Morning на CBS. Щомісяця веб-сайт Джеймса відвідують мільйони людей, а сотні тисяч підписані на його популярну електронну розсилку новин. Кліра як лектора постійно запрошують виступити компанії, що входять до списку Fortune 500, а його ідеї і напрацювання використовують члени команд Національної футбольної ліги, Національної баскетбольної асоціації і Головної ліги бейсболу. На його онлайн-курсі The Habits Academy пройшли навчання понад десять тисяч керівників, тренерів і вчителів. The Habits Academy - провідна освітня платформа, призначена як для приватних осіб, так і для працівників організацій, тобто усіх, хто прагне набути кращих звичок у побуті, особистій і професійній сферах. "Надзвичайно практична й корисна книжка. Джеймс Клір виокремив найголовніше щодо формування звичок, тож ви зможете досягти більшого, докладаючи менших зусиль". - Марк Менсон, автор книги "Витончене мистецтво забивати на все. Нестандартний підхід до проблем", бестселера за версією "The New York Times" "Джеймс Клір роками відточував майстерність і таки опанував науку звичок. Захоплива книжка, заснована на практичних досягненнях, є тим дороговказом, який потрібен вам, щоб зруйнувати шкідливий розпорядок дня і виробити корисний". - Адам Грант, автор бестселерів "The New York Times" "Оригінали", "Взаємопоступки" та "Можливість" у співавторстві з Шеріл Сандберг "Особливе видання, яке змінить ваш підхід до щоденних справ і життя загалом". - Раян Голідей, автор бестселерів "The New York Times" "Напролом. Мистецтво перетворювати перешкоди на перемоги" та "Его - ворог"	Psychology	http://books.google.com/books/content?id=sm_-EAAAQBAJ&printsec=frontcover&img=1&zoom=1&edge=curl&source=gbs_api	GOOGLE_BOOKS	sm_-EAAAQBAJ	2026-10-19 17:36:10.124293
\.


--
-- Data for Name: chat_messages; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.chat_messages (id, chat_room_id, sender_id, content, created_at, read_at) FROM stdin;
f9d0d6f1-bc69-4002-963e-18b7c3b86d2d	f6666666-0000-0000-0000-000000000001	11111111-1111-1111-1111-111111111111	Привіт! Відправив книгу поштою сьогодні.	2026-09-12 05:43:14.569194	2026-09-12 07:43:14.569194
927b5027-a3da-4370-a405-d4092fb125ac	f6666666-0000-0000-0000-000000000001	22222222-2222-2222-2222-222222222222	Дякую, чекаю на трек-номер!	2026-09-12 06:43:14.569194	2026-09-12 08:43:14.569194
1007f658-74b1-43d8-b8f8-74edb95a97e7	f6666666-0000-0000-0000-000000000001	11111111-1111-1111-1111-111111111111	Ось накладна, вже прикріпив у систему.	2026-09-12 07:43:14.569194	\N
cbf6aa49-ad26-48b9-ba17-a737e815b550	7b593bc9-3eaf-4ad2-b338-68c2fa3355b3	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	Привіт	2026-10-19 15:55:24.946702	2026-10-19 17:49:39.639446
ebd92b36-79a5-4900-b5aa-d76a558ba73f	7b593bc9-3eaf-4ad2-b338-68c2fa3355b3	4d909f2a-4d9a-43fb-9ba4-273c6270b2a0	Привіт	2026-10-27 15:43:28.930155	\N
\.


--
-- Data for Name: chat_rooms; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.chat_rooms (id, user_a_id, user_b_id, created_at) FROM stdin;
f6666666-0000-0000-0000-000000000001	11111111-1111-1111-1111-111111111111	22222222-2222-2222-2222-222222222222	2026-09-11 05:43:14.546653
7b593bc9-3eaf-4ad2-b338-68c2fa3355b3	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	4d909f2a-4d9a-43fb-9ba4-273c6270b2a0	2026-10-16 08:56:03.586784
0f03e17e-c1df-4666-836a-57271125bebd	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	11111111-1111-1111-1111-111111111111	2026-09-27 05:59:24.686192
b1644b1e-d17f-4e57-9d1e-3327a2d17af3	33333333-3333-3333-3333-333333333333	22222222-2222-2222-2222-222222222222	2026-10-01 05:43:14.27248
bbe29057-cbf5-4520-843e-a70ad23d5cf2	22222222-2222-2222-2222-222222222222	44444444-4444-4444-4444-444444444444	2026-08-07 05:43:14.388054
0ff3f157-3820-4f0e-bbde-f08f6d020bc2	22222222-2222-2222-2222-222222222222	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	2026-09-14 05:59:24.686192
05b44cb8-4bab-45bd-b651-d2246f5e0f44	44444444-4444-4444-4444-444444444444	55555555-5555-5555-5555-555555555555	2026-07-28 05:43:14.475059
\.


--
-- Data for Name: comments; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.comments (id, post_id, author_id, content, created_at) FROM stdin;
015e4b2f-978e-45b2-96e9-bd569ad7bb71	11223344-0000-0000-0000-000000000001	22222222-2222-2222-2222-222222222222	Теж хочу прочитати, побачила у вас в оголошеннях!	2026-09-21 05:43:14.622218
236dced5-fc45-4d70-aa9a-9fc5d7a0331c	11223344-0000-0000-0000-000000000002	33333333-3333-3333-3333-333333333333	Клас, а "Атомні звички" ще доступні?	2026-09-28 05:43:14.622218
5810000d-0d8d-4e85-99e5-2a2c502f80d7	11223344-0000-0000-0000-000000000002	4d909f2a-4d9a-43fb-9ba4-273c6270b2a0	Супер!	2026-10-19 17:50:58.718736
\.


--
-- Data for Name: deadline_extension_requests; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.deadline_extension_requests (id, exchange_id, requested_new_deadline, status, comment, created_at, decided_at, decision_comment, extra_days) FROM stdin;
3236b7a3-f371-429b-a7fd-65f0af9160c9	1dac1b3d-76c3-4610-9edb-6ff626528da3	2027-01-14	APPROVED	\N	2026-10-19 17:44:23.796557	2026-10-19 17:47:55.622491	\N	7
5e973c84-29d7-408b-b524-55d5e5924531	f3a30000-0000-0000-0000-000000000005	\N	REJECTED	\N	2026-10-19 17:49:50.753517	2026-10-19 17:50:29.009784	Розраховую отримати назад чимшвидше	30
34323999-0e58-4935-b290-8bc64475fdf0	f3a30000-0000-0000-0000-000000000002	2026-11-12	APPROVED	\N	2026-10-27 10:58:36.670796	2026-10-27 15:43:22.836299	\N	30
\.


--
-- Data for Name: deadline_reminders; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.deadline_reminders (exchange_id, kind, due_date, sent_at) FROM stdin;
d4444444-0000-0000-0000-000000000001	OVERDUE	2026-10-19	2026-10-26 12:05:00.067048
f3a30000-0000-0000-0000-000000000002	OVERDUE	2026-10-13	2026-10-26 12:05:00.146304
f3a30000-0000-0000-0000-000000000005	OVERDUE	2026-09-27	2026-10-26 12:05:00.314507
f3a30000-0000-0000-0000-000000000007	OVERDUE	2026-10-11	2026-10-26 12:05:00.331891
\.


--
-- Data for Name: disputes; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.disputes (id, exchange_id, filed_by, description, status, moderator_id, resolution_comment, created_at, resolved_at) FROM stdin;
e5555555-0000-0000-0000-000000000001	d4444444-0000-0000-0000-000000000004	44444444-4444-4444-4444-444444444444	Книгу повернули з пошкодженою обкладинкою та підкресленнями, яких не було задекларовано.	RESOLVED_FAVOR_FILER	66666666-6666-6666-6666-666666666666	Підтверджено пошкодження за фото. Читачу зараховано порушення.	2026-08-31 05:43:14.499904	2026-09-02 05:43:14.499904
\.


--
-- Data for Name: exchange_photos; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.exchange_photos (id, exchange_id, uploaded_by, stage, url, note, created_at) FROM stdin;
2bc958f9-f1dc-48a6-a4c1-389a899c2c2f	d4444444-0000-0000-0000-000000000001	11111111-1111-1111-1111-111111111111	HANDOVER	https://photos.example.com/exch1_owner_1.jpg	Стан задокументовано перед відправкою.	2026-10-05 05:43:14.305227
50249f86-f25c-472b-8fb7-18f9b81077ae	d4444444-0000-0000-0000-000000000001	22222222-2222-2222-2222-222222222222	HANDOVER	https://photos.example.com/exch1_reader_1.jpg	Отримано, недоліків не виявлено.	2026-10-05 05:43:14.305227
f4a40000-0000-0000-0000-000000000001	f3a30000-0000-0000-0000-000000000002	4d909f2a-4d9a-43fb-9ba4-273c6270b2a0	HANDOVER	https://picsum.photos/seed/exch2owner/400/600	Стан перед відправкою.	2026-09-26 05:59:24.686192
f4a40000-0000-0000-0000-000000000002	f3a30000-0000-0000-0000-000000000002	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	HANDOVER	https://picsum.photos/seed/exch2reader/400/600	Отримано, усе гаразд.	2026-09-27 05:59:24.686192
f4a40000-0000-0000-0000-000000000003	f3a30000-0000-0000-0000-000000000003	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	HANDOVER	https://picsum.photos/seed/exch3owner/400/600	\N	2026-09-21 05:59:24.686192
f4a40000-0000-0000-0000-000000000004	f3a30000-0000-0000-0000-000000000003	4d909f2a-4d9a-43fb-9ba4-273c6270b2a0	HANDOVER	https://picsum.photos/seed/exch3reader/400/600	\N	2026-09-22 05:59:24.686192
f4a40000-0000-0000-0000-000000000005	f3a30000-0000-0000-0000-000000000003	4d909f2a-4d9a-43fb-9ba4-273c6270b2a0	RETURN	https://picsum.photos/seed/exch3return/400/600	Повертаю, читала акуратно.	2026-10-04 05:59:24.686192
f4a40000-0000-0000-0000-000000000006	f3a30000-0000-0000-0000-000000000004	4d909f2a-4d9a-43fb-9ba4-273c6270b2a0	HANDOVER	https://picsum.photos/seed/exch4owner/400/600	\N	2026-09-16 05:59:24.686192
f4a40000-0000-0000-0000-000000000007	f3a30000-0000-0000-0000-000000000004	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	HANDOVER	https://picsum.photos/seed/exch4reader/400/600	\N	2026-09-17 05:59:24.686192
f4a40000-0000-0000-0000-000000000008	f3a30000-0000-0000-0000-000000000004	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	RETURN	https://picsum.photos/seed/exch4return1/400/600	\N	2026-09-30 05:59:24.686192
f4a40000-0000-0000-0000-000000000009	f3a30000-0000-0000-0000-000000000004	4d909f2a-4d9a-43fb-9ba4-273c6270b2a0	RETURN	https://picsum.photos/seed/exch4return2/400/600	Прийнято без зауважень.	2026-10-01 05:59:24.686192
f4a40000-0000-0000-0000-000000000010	f3a30000-0000-0000-0000-000000000005	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	HANDOVER	https://picsum.photos/seed/exch5owner/400/600	\N	2026-09-11 05:59:24.686192
f4a40000-0000-0000-0000-000000000011	f3a30000-0000-0000-0000-000000000005	4d909f2a-4d9a-43fb-9ba4-273c6270b2a0	HANDOVER	https://picsum.photos/seed/exch5reader/400/600	\N	2026-09-12 05:59:24.686192
f4a40000-0000-0000-0000-000000000012	f3a30000-0000-0000-0000-000000000006	4d909f2a-4d9a-43fb-9ba4-273c6270b2a0	HANDOVER	https://picsum.photos/seed/exch6owner/400/600	\N	2026-09-18 05:59:24.686192
f4a40000-0000-0000-0000-000000000013	f3a30000-0000-0000-0000-000000000006	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	HANDOVER	https://picsum.photos/seed/exch6reader/400/600	\N	2026-09-19 05:59:24.686192
f4a40000-0000-0000-0000-000000000014	f3a30000-0000-0000-0000-000000000006	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	RETURN	https://picsum.photos/seed/exch6return/400/600	\N	2026-10-02 05:59:24.686192
f4a40000-0000-0000-0000-000000000015	f3a30000-0000-0000-0000-000000000006	4d909f2a-4d9a-43fb-9ba4-273c6270b2a0	DISPUTE	https://picsum.photos/seed/exch6dispute1/400/600	Сторінки порвані, плями на обкладинці.	2026-10-03 05:59:24.686192
f4a40000-0000-0000-0000-000000000016	f3a30000-0000-0000-0000-000000000006	4d909f2a-4d9a-43fb-9ba4-273c6270b2a0	DISPUTE	https://picsum.photos/seed/exch6dispute2/400/600	Крупний план пошкодження.	2026-10-03 05:59:24.686192
f4a40000-0000-0000-0000-000000000017	f3a30000-0000-0000-0000-000000000007	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	HANDOVER	https://picsum.photos/seed/exch7owner/400/600	\N	2026-09-27 05:59:24.686192
f4a40000-0000-0000-0000-000000000018	f3a30000-0000-0000-0000-000000000007	11111111-1111-1111-1111-111111111111	HANDOVER	https://picsum.photos/seed/exch7reader/400/600	\N	2026-09-28 05:59:24.686192
f4a40000-0000-0000-0000-000000000019	f3a30000-0000-0000-0000-000000000008	22222222-2222-2222-2222-222222222222	HANDOVER	https://picsum.photos/seed/exch8owner/400/600	\N	2026-09-14 05:59:24.686192
f4a40000-0000-0000-0000-000000000020	f3a30000-0000-0000-0000-000000000008	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	HANDOVER	https://picsum.photos/seed/exch8reader/400/600	\N	2026-09-15 05:59:24.686192
f4a40000-0000-0000-0000-000000000021	f3a30000-0000-0000-0000-000000000008	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	RETURN	https://picsum.photos/seed/exch8return1/400/600	\N	2026-09-28 05:59:24.686192
f4a40000-0000-0000-0000-000000000022	f3a30000-0000-0000-0000-000000000008	22222222-2222-2222-2222-222222222222	RETURN	https://picsum.photos/seed/exch8return2/400/600	\N	2026-09-29 05:59:24.686192
9b00633e-ca2a-405e-999f-0b2e8249c574	1dac1b3d-76c3-4610-9edb-6ff626528da3	4d909f2a-4d9a-43fb-9ba4-273c6270b2a0	HANDOVER	http://localhost:8080/uploads/c48caf33-df06-40af-bbb0-fed302dee103.png	\N	2026-10-19 17:39:54.894192
1f46e541-91e0-46cb-8724-36fabf429a94	1dac1b3d-76c3-4610-9edb-6ff626528da3	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	HANDOVER	http://localhost:8080/uploads/2d03a013-05c8-4b02-b5c2-db62228d1226.png	\N	2026-10-19 17:42:19.729122
b54970d9-bba9-4c88-981f-2961c3c08419	1dac1b3d-76c3-4610-9edb-6ff626528da3	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	RETURN	http://localhost:8080/uploads/e3b133c2-4aa2-4c7f-810a-e0b0ebb80641.png	\N	2026-10-19 17:48:16.47123
\.


--
-- Data for Name: exchanges; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.exchanges (id, request_id, listing_id, owner_id, reader_id, delivery_method, deadline, extended_deadline, status, dispute_reason, created_at, completed_at, reading_days, reading_started_at, return_prepared_at, returned_at, reading_paused_days) FROM stdin;
d4444444-0000-0000-0000-000000000003	c3333333-0000-0000-0000-000000000005	b2222222-0000-0000-0000-000000000002	22222222-2222-2222-2222-222222222222	44444444-4444-4444-4444-444444444444	PICKUP	2026-09-25	\N	COMPLETED	\N	2026-08-07 05:43:14.388054	2026-09-23 05:43:14.388054	50	\N	\N	\N	0
d4444444-0000-0000-0000-000000000004	c3333333-0000-0000-0000-000000000006	b2222222-0000-0000-0000-000000000003	44444444-4444-4444-4444-444444444444	55555555-5555-5555-5555-555555555555	PICKUP	2026-08-26	\N	DISPUTED	\N	2026-07-28 05:43:14.475059	\N	30	\N	\N	\N	0
1dac1b3d-76c3-4610-9edb-6ff626528da3	e8c068b5-25d5-43e4-8f6d-6eee0531bd63	4c215420-d0c0-4781-9ce5-2c0ec598b64e	4d909f2a-4d9a-43fb-9ba4-273c6270b2a0	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	MAIL	2027-01-07	2027-01-14	COMPLETED	\N	2026-10-19 17:38:13.064942	2026-10-19 17:49:21.804633	80	2026-10-19 17:42:19.728053	2026-10-19 17:48:16.470713	2026-10-19 17:49:05.454166	0
f3a30000-0000-0000-0000-000000000002	f2a20000-0000-0000-0000-000000000005	f1a10000-0000-0000-0000-000000000005	4d909f2a-4d9a-43fb-9ba4-273c6270b2a0	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	MAIL	2026-10-13	2026-11-12	IN_READING	\N	2026-09-26 05:59:24.686192	\N	18	2026-09-27 05:59:24.686192	\N	\N	0
d4444444-0000-0000-0000-000000000001	c3333333-0000-0000-0000-000000000001	b2222222-0000-0000-0000-000000000001	11111111-1111-1111-1111-111111111111	22222222-2222-2222-2222-222222222222	MAIL	2026-10-19	\N	IN_READING	\N	2026-09-11 05:43:14.27248	\N	39	2026-10-05 05:43:14.305227	\N	\N	0
f3a30000-0000-0000-0000-000000000005	f2a20000-0000-0000-0000-000000000008	f1a10000-0000-0000-0000-000000000008	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	4d909f2a-4d9a-43fb-9ba4-273c6270b2a0	PICKUP	2026-09-27	\N	OVERDUE	\N	2026-09-11 05:59:24.686192	\N	17	2026-09-12 05:59:24.686192	\N	\N	0
f3a30000-0000-0000-0000-000000000007	f2a20000-0000-0000-0000-000000000010	f1a10000-0000-0000-0000-000000000010	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	11111111-1111-1111-1111-111111111111	PICKUP	2026-10-11	\N	IN_READING	\N	2026-09-27 05:59:24.686192	\N	15	2026-09-28 05:59:24.686192	\N	\N	0
d4444444-0000-0000-0000-000000000002	c3333333-0000-0000-0000-000000000002	b2222222-0000-0000-0000-000000000004	33333333-3333-3333-3333-333333333333	22222222-2222-2222-2222-222222222222	MAIL	\N	\N	HANDOVER_PENDING	\N	2026-10-01 05:43:14.27248	\N	15	\N	\N	\N	0
f3a30000-0000-0000-0000-000000000001	f2a20000-0000-0000-0000-000000000004	f1a10000-0000-0000-0000-000000000004	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	4d909f2a-4d9a-43fb-9ba4-273c6270b2a0	PICKUP	\N	\N	HANDOVER_PENDING	\N	2026-10-02 05:59:24.686192	\N	16	\N	\N	\N	0
2112cac6-f686-4d49-89f7-6c790cb4811b	f2a20000-0000-0000-0000-000000000001	f1a10000-0000-0000-0000-000000000001	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	4d909f2a-4d9a-43fb-9ba4-273c6270b2a0	PICKUP	\N	\N	HANDOVER_PENDING	\N	2026-10-16 08:56:03.582284	\N	16	\N	\N	\N	0
f3a30000-0000-0000-0000-000000000003	f2a20000-0000-0000-0000-000000000006	f1a10000-0000-0000-0000-000000000006	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	4d909f2a-4d9a-43fb-9ba4-273c6270b2a0	MAIL	2026-10-07	\N	RETURN_PENDING	\N	2026-09-21 05:59:24.686192	\N	17	2026-09-22 05:59:24.686192	2026-10-04 05:59:24.686192	\N	0
f3a30000-0000-0000-0000-000000000006	f2a20000-0000-0000-0000-000000000009	f1a10000-0000-0000-0000-000000000009	4d909f2a-4d9a-43fb-9ba4-273c6270b2a0	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	MAIL	2026-10-03	\N	DISPUTED	Книга повернута з розірваними сторінками та плямами, яких не було задекларовано власником спочатку.	2026-09-18 05:59:24.686192	\N	16	2026-09-19 05:59:24.686192	2026-10-02 05:59:24.686192	2026-10-04 05:59:24.686192	0
f3a30000-0000-0000-0000-000000000004	f2a20000-0000-0000-0000-000000000007	f1a10000-0000-0000-0000-000000000007	4d909f2a-4d9a-43fb-9ba4-273c6270b2a0	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	PICKUP	2026-09-30	\N	COMPLETED	\N	2026-09-16 05:59:24.686192	2026-10-01 05:59:24.686192	15	2026-09-17 05:59:24.686192	2026-09-30 05:59:24.686192	2026-09-30 05:59:24.686192	0
f3a30000-0000-0000-0000-000000000008	f2a20000-0000-0000-0000-000000000011	f1a10000-0000-0000-0000-000000000011	22222222-2222-2222-2222-222222222222	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	PICKUP	2026-09-25	\N	COMPLETED	\N	2026-09-14 05:59:24.686192	2026-09-29 05:59:24.686192	12	2026-09-15 05:59:24.686192	2026-09-28 05:59:24.686192	2026-09-28 05:59:24.686192	0
\.


--
-- Data for Name: feedback; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.feedback (id, topic, email, message, link, screenshot_url, user_id, ip_address, status, moderator_note, handled_by, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: listing_photos; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.listing_photos (id, listing_id, url, created_at) FROM stdin;
d72bf8f7-fac6-4259-ad37-762208aa40b0	b2222222-0000-0000-0000-000000000001	https://photos.example.com/listing1_1.jpg	2026-10-05 05:43:14.131382
73766cd7-0688-415f-95c6-4ab097bc687c	b2222222-0000-0000-0000-000000000001	https://photos.example.com/listing1_2.jpg	2026-10-05 05:43:14.131382
c2ec524d-fc36-4cc3-bc87-9dd9892bec6c	b2222222-0000-0000-0000-000000000002	https://photos.example.com/listing2_1.jpg	2026-10-05 05:43:14.131382
2d0465de-17b9-476a-8864-e9ab7ee9f577	b2222222-0000-0000-0000-000000000003	https://photos.example.com/listing3_1.jpg	2026-10-05 05:43:14.131382
a584dd75-ba58-4016-862a-485c1621f9f8	b2222222-0000-0000-0000-000000000004	https://photos.example.com/listing4_1.jpg	2026-10-05 05:43:14.131382
f7a70000-0000-0000-0000-000000000001	f1a10000-0000-0000-0000-000000000001	https://picsum.photos/seed/listing1a/400/600	2026-09-30 05:59:24.686192
f7a70000-0000-0000-0000-000000000002	f1a10000-0000-0000-0000-000000000004	https://picsum.photos/seed/listing4a/400/600	2026-10-01 05:59:24.686192
f7a70000-0000-0000-0000-000000000003	f1a10000-0000-0000-0000-000000000007	https://picsum.photos/seed/listing7a/400/600	2026-09-15 05:59:24.686192
3b53733d-a530-4de3-93c4-bf8d7000358d	b2222222-0000-0000-0000-000000000001	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
03951ee3-86a9-4f51-8ce1-2b1f6ec7a735	b2222222-0000-0000-0000-000000000001	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
56ef09e8-320e-48cc-bf2d-34d06839eee0	b2222222-0000-0000-0000-000000000002	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
d9993df0-9584-4106-84a3-bfa27c71be3e	b2222222-0000-0000-0000-000000000002	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
7c81bf17-7b18-419e-a31a-56a7467ced2b	b2222222-0000-0000-0000-000000000002	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
3d838211-a5f4-409c-9b80-6beda3f76b1d	b2222222-0000-0000-0000-000000000003	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
80180cc3-1730-45e4-9b04-40d2ac907f30	b2222222-0000-0000-0000-000000000003	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
f740b6ab-bcf4-488e-9ab1-14a1e77e011a	b2222222-0000-0000-0000-000000000003	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
3a443676-1d3c-4318-8dd6-3ba76968cfe8	b2222222-0000-0000-0000-000000000004	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
55dc1cb8-aac3-4972-b04d-d7d840f654cf	b2222222-0000-0000-0000-000000000004	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
d62605c0-55cd-44bb-85d9-b7bebfb767fd	b2222222-0000-0000-0000-000000000004	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
97a4482b-6f79-44aa-8162-d5fa571b71c9	401add04-e443-41e7-9800-75810398a42d	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
6978fd11-c71a-4b40-9c09-6f23273c66ca	401add04-e443-41e7-9800-75810398a42d	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
e3b62c91-fdf5-4e35-ac4b-8c8e86c87828	401add04-e443-41e7-9800-75810398a42d	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
57c1cee9-1c28-4bc5-b2a3-956f49d7460a	401add04-e443-41e7-9800-75810398a42d	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
9e370728-d8d1-4d22-b199-b8ed854b1166	8cf403d3-1075-4361-87e3-f95b4fa56bb8	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
e3c4cdde-3873-4982-98b7-eb19a6f4ba69	8cf403d3-1075-4361-87e3-f95b4fa56bb8	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
7e0d3f14-49f1-474c-8b6e-ac3b7cc8f6cc	8cf403d3-1075-4361-87e3-f95b4fa56bb8	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
b4a4895a-ff7a-45fe-b423-b476226cc0d3	8cf403d3-1075-4361-87e3-f95b4fa56bb8	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
62aa6475-3131-4c81-b5bc-03bc0cccb1e0	699ee919-6aac-4f3f-9d3d-53a6e0483c9f	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
be7c9b87-e452-466a-a109-8bcf87ac2758	699ee919-6aac-4f3f-9d3d-53a6e0483c9f	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
1e23b2b5-ed77-4507-b1d9-1b531173e6a3	699ee919-6aac-4f3f-9d3d-53a6e0483c9f	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
031286be-b858-407b-adc1-0f8a4159b20f	699ee919-6aac-4f3f-9d3d-53a6e0483c9f	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
c11a4f6c-4f2c-4af4-9da0-f0d56d7e82f4	490cd468-99f2-49cf-a63f-eed79e41da04	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
da88330b-420a-4e24-a65d-75ab73b050ab	490cd468-99f2-49cf-a63f-eed79e41da04	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
75262fc2-f0ed-4fe4-9a23-7c252c938c64	490cd468-99f2-49cf-a63f-eed79e41da04	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
1f3187aa-60ba-4d3f-befc-da06adc7aa23	490cd468-99f2-49cf-a63f-eed79e41da04	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
eff052a9-3555-42cf-8d9d-3fe340dda617	73ae542a-a1cf-4378-ac97-08f8ae8315a1	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
eb9eb4e3-b450-4fed-a6f2-b493dbd513a2	73ae542a-a1cf-4378-ac97-08f8ae8315a1	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
fbf02193-a50a-49b1-8751-d7da0257d0c6	73ae542a-a1cf-4378-ac97-08f8ae8315a1	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
87342678-2087-4b90-8d54-b9badc766b68	73ae542a-a1cf-4378-ac97-08f8ae8315a1	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
1e25f783-976f-4e0d-b940-5629dd2ccd36	ed0e657f-4656-4567-83a7-56ca47a4d00a	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
d2c23a08-5193-4def-bc09-ecd310f39df2	ed0e657f-4656-4567-83a7-56ca47a4d00a	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
14bcc426-0c73-4426-9722-6f1d015cd58a	ed0e657f-4656-4567-83a7-56ca47a4d00a	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
d3289870-0a83-4e0c-a2ef-90fbc3f4f9cf	ed0e657f-4656-4567-83a7-56ca47a4d00a	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
1f586bbd-4f0d-4bf4-a2f4-e176a6c7d67b	f2e8e325-d074-4967-901f-4cf9b0a0ffe9	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
a92f7c6e-cd88-469e-8788-33b0d035cdd7	f2e8e325-d074-4967-901f-4cf9b0a0ffe9	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
0c1bdc6d-2c78-431d-8061-9547e2ca0897	f2e8e325-d074-4967-901f-4cf9b0a0ffe9	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
571f4056-e16b-4856-b48e-e7d89c0b9ab4	f2e8e325-d074-4967-901f-4cf9b0a0ffe9	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
782faec0-d246-4e48-a823-d9ba234d51d0	c640c4a5-07de-41f6-8777-62565392c8cb	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
a9c9b0c5-cee3-41f7-b9b5-4992e303824c	c640c4a5-07de-41f6-8777-62565392c8cb	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
20692f40-2da6-4fde-96d2-439fa893c462	c640c4a5-07de-41f6-8777-62565392c8cb	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
a25e35b5-76a4-4052-b3db-35b8c7db6a7e	c640c4a5-07de-41f6-8777-62565392c8cb	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
4b4a6eb1-ddc8-4d3c-a29b-7d51b34467d9	dfd39ae4-55a2-4892-a8ec-1fbfa4ea5f1a	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
bbd04f1d-0d8d-40f9-8712-fe25ca6c7ed4	dfd39ae4-55a2-4892-a8ec-1fbfa4ea5f1a	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
5c2110ea-7308-4a58-938d-bcf11ec6f4a1	dfd39ae4-55a2-4892-a8ec-1fbfa4ea5f1a	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
167cb53b-fff2-4596-bfc0-c9f121175090	dfd39ae4-55a2-4892-a8ec-1fbfa4ea5f1a	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
1bac62af-df92-49e9-b11b-b1637fc59f97	7467a0ed-e101-43c6-bd1e-b1e9e03f0d2d	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
048b84ef-2603-423b-9a38-365bda916b10	7467a0ed-e101-43c6-bd1e-b1e9e03f0d2d	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
76feaffa-6031-489a-b811-a06b138e9358	7467a0ed-e101-43c6-bd1e-b1e9e03f0d2d	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
f8899671-cdae-4475-966d-af90eb28e8c5	7467a0ed-e101-43c6-bd1e-b1e9e03f0d2d	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
3ecb1d75-6d56-4321-9d74-d6ad6ec0aa30	69727724-d0b7-4a18-9ce0-4076b0f3afa9	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
1f206532-2c49-4abd-ab34-92415a0780b8	69727724-d0b7-4a18-9ce0-4076b0f3afa9	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
2eecedf4-4293-435c-bd43-7a49eb47004c	69727724-d0b7-4a18-9ce0-4076b0f3afa9	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
c4537771-51e7-48eb-a83a-02f6c287512e	69727724-d0b7-4a18-9ce0-4076b0f3afa9	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
d5415724-a473-451c-b700-f6dbbbce9d08	a3a3e9de-bffa-4ed5-8e96-22acfe58cf2d	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
dc4b9c08-b6ab-4936-8c74-ae682c62b687	a3a3e9de-bffa-4ed5-8e96-22acfe58cf2d	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
2534d164-79ff-4253-944a-f20ebd88d585	a3a3e9de-bffa-4ed5-8e96-22acfe58cf2d	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
6f38431c-955d-4794-9bfc-ccb42855a359	a3a3e9de-bffa-4ed5-8e96-22acfe58cf2d	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
6b4ced1e-24ff-4109-bd47-d06dcd0facdf	18abb25c-4c5b-4b4f-9c8f-332c79f0fbac	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
9275c091-262e-46eb-89d1-53e32f601029	18abb25c-4c5b-4b4f-9c8f-332c79f0fbac	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
32a49598-3e3e-4f99-a9a8-70630c1ab5ac	18abb25c-4c5b-4b4f-9c8f-332c79f0fbac	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
e8837750-cf99-4802-b64f-b3f94ef3b264	18abb25c-4c5b-4b4f-9c8f-332c79f0fbac	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
29c7be8e-2733-428e-9b93-15d6370530e6	a424e05a-b417-4118-bd21-143cdb9da0bb	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
7204d23d-5d6e-4fad-aee8-a7a728ad0154	a424e05a-b417-4118-bd21-143cdb9da0bb	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
1e49e242-e13c-454b-b9f9-af4158962437	a424e05a-b417-4118-bd21-143cdb9da0bb	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
8e9db327-d641-4d1a-9d22-5eb5c8570c61	a424e05a-b417-4118-bd21-143cdb9da0bb	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
642b2766-d333-43f7-9786-344879ca4b93	7a808062-2721-4dc1-9ad8-07b9fb0524f1	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
10230c42-0452-4bcf-861f-1f7df79f218c	7a808062-2721-4dc1-9ad8-07b9fb0524f1	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
3bbcd79f-1de0-4a30-b41e-e74c1cb452e6	7a808062-2721-4dc1-9ad8-07b9fb0524f1	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
b3a5c2c9-cec7-4b1e-896c-e3d53f3a671e	7a808062-2721-4dc1-9ad8-07b9fb0524f1	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
064e1dab-50a7-4e22-ad17-11e2ea77ee36	195a8bab-41fe-4340-ad7e-1b3f6a215b22	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
d2970e7f-c967-486e-b3b3-171728a85ceb	195a8bab-41fe-4340-ad7e-1b3f6a215b22	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
c112d3c0-df36-460f-9dce-810743bb11b2	195a8bab-41fe-4340-ad7e-1b3f6a215b22	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
58b6c840-d2a5-4440-95fa-f06a83d29603	195a8bab-41fe-4340-ad7e-1b3f6a215b22	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
7d15f87e-0a06-4e6e-8c11-5bb003a2c4ef	e1e9b9f9-9531-4a05-8689-b52cc88d5e47	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
af448638-35ed-4434-80ab-69f83f064f00	e1e9b9f9-9531-4a05-8689-b52cc88d5e47	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
f49c2a83-7d23-41ca-bee5-1bf889099647	e1e9b9f9-9531-4a05-8689-b52cc88d5e47	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
e215c9b9-9efe-41ca-9be9-878bcef86bd1	e1e9b9f9-9531-4a05-8689-b52cc88d5e47	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
79db5873-f0c4-4238-85cf-6fe5b16215b2	8ff58015-e670-483f-8897-2fb4b251766d	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
6d33cc6e-aa23-4d03-9951-deaea6524e22	8ff58015-e670-483f-8897-2fb4b251766d	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
30c474db-b8b9-4c3f-9593-36265c0bf048	8ff58015-e670-483f-8897-2fb4b251766d	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
91bc838d-ee5a-4e18-837e-9071f42dcaac	8ff58015-e670-483f-8897-2fb4b251766d	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
c61986f7-fc1b-4a8f-af7c-aa49ae27926a	d9dfe2e4-4c87-4a3e-838d-e8a5b855a0ca	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
0bae5ad5-b457-442e-9b65-73a5a85bbbcd	d9dfe2e4-4c87-4a3e-838d-e8a5b855a0ca	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
1c5d26f2-58bf-4510-b610-223263c4f5dc	d9dfe2e4-4c87-4a3e-838d-e8a5b855a0ca	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
53675838-68fe-473c-8ec6-dc3a60c04441	d9dfe2e4-4c87-4a3e-838d-e8a5b855a0ca	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
7b0d4b20-fec5-4938-9bff-3c1233788a28	66b25739-a872-4299-a3e0-7b257454d1ff	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
ad680fa2-cec1-4698-8c21-9facc912c566	66b25739-a872-4299-a3e0-7b257454d1ff	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
0cdd68ac-16b0-449c-a4d8-cfa18a49c686	66b25739-a872-4299-a3e0-7b257454d1ff	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
6f9d4990-016d-4a94-9128-a02dfb97235f	66b25739-a872-4299-a3e0-7b257454d1ff	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
4d88941e-76df-457c-8fe5-951ba45b323d	209f62e4-a6f2-42ef-847d-65ee172f26de	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
56d8a3ec-7df7-4a25-a825-a43e8bbdff92	209f62e4-a6f2-42ef-847d-65ee172f26de	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
b0ca35b6-391c-47b8-809c-da11f288b500	209f62e4-a6f2-42ef-847d-65ee172f26de	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
53e512cb-80fc-4b08-a542-ff39c21f6792	209f62e4-a6f2-42ef-847d-65ee172f26de	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
993b65bb-1634-467b-897e-0d977ed3f518	3f3f8b6e-025d-450d-acc7-8365049f31ab	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
67b26d77-4381-4f06-9db6-3aee77f9790e	3f3f8b6e-025d-450d-acc7-8365049f31ab	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
f9a407f3-ad57-4f75-add3-f8afad2a4d92	3f3f8b6e-025d-450d-acc7-8365049f31ab	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
adbd2961-6971-4131-9ce5-cd11475231d1	3f3f8b6e-025d-450d-acc7-8365049f31ab	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
9e917983-ed17-4178-aa90-843724ec6e4a	7161cfb9-f28f-4a16-a094-389f27aa8e56	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
6a4ff7f4-edf4-4bd4-b23a-0fe275ce74a6	7161cfb9-f28f-4a16-a094-389f27aa8e56	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
92599ff2-60b8-42e7-8285-d88a185b4e23	7161cfb9-f28f-4a16-a094-389f27aa8e56	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
27c25ebf-6fcb-4077-9462-87349c401f3f	7161cfb9-f28f-4a16-a094-389f27aa8e56	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
e7b3360b-3487-4ab4-aa76-0fe9969dfe58	cae6e485-adda-472c-809e-f91c8e74c7e2	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
c5f6e27c-fb48-43f1-9212-775817884d7d	cae6e485-adda-472c-809e-f91c8e74c7e2	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
9eec7548-cf1b-40f6-b32d-c8b3fcd13db9	cae6e485-adda-472c-809e-f91c8e74c7e2	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
25fa501c-0317-416e-bb3e-0ac7367da641	cae6e485-adda-472c-809e-f91c8e74c7e2	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
1cafa469-7a66-4886-91aa-a234d090ee06	01d2e712-e0a9-40e5-a86c-9992da33f01c	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
1785f6ca-073a-45b6-a3fe-fe6cbe71e4ec	01d2e712-e0a9-40e5-a86c-9992da33f01c	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
e8ea291d-c987-4e8d-a0b9-53c09d0a013a	01d2e712-e0a9-40e5-a86c-9992da33f01c	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
6bc5a8c4-64fc-4d80-a34c-d53b41b3308d	01d2e712-e0a9-40e5-a86c-9992da33f01c	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
690a04e5-52ea-4bcd-aa5e-e2f5007a43b8	2ba5567c-1f5b-4ca7-9e59-b9ac30412dd7	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
636dfed9-fb71-4681-a2aa-7b22c2a272ee	2ba5567c-1f5b-4ca7-9e59-b9ac30412dd7	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
deb4429b-9ef3-459d-85b2-a0be9cf4e967	2ba5567c-1f5b-4ca7-9e59-b9ac30412dd7	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
36e28afb-02bf-4b28-82d4-03aa64491998	2ba5567c-1f5b-4ca7-9e59-b9ac30412dd7	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
68a0cc22-5fc8-4399-bde3-e95e166ac052	18ec9025-e2bf-4363-b985-cb015740e041	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
a1d8f0b1-9436-4471-b8ec-0fab4b72b51b	18ec9025-e2bf-4363-b985-cb015740e041	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
db923ff8-e495-495f-9158-17de5ace332b	18ec9025-e2bf-4363-b985-cb015740e041	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
f68e8398-044f-4301-be30-8a5e2911f1f3	18ec9025-e2bf-4363-b985-cb015740e041	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
1e8bdb7e-4b10-460e-8174-9f7670bd0252	286b58dc-ea9b-453e-879a-b6dcbe946bae	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
65a16dcc-e51d-4ccf-88d2-93cca3a75214	286b58dc-ea9b-453e-879a-b6dcbe946bae	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
e3a30158-767e-4289-bf64-7551785e0f7c	286b58dc-ea9b-453e-879a-b6dcbe946bae	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
5a72928b-b94e-42f7-85a4-ea52edebff6d	286b58dc-ea9b-453e-879a-b6dcbe946bae	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
8f6e03b9-d873-4d26-b58e-83269deccd2c	8876bf92-d666-4326-8d0c-46798c8c0c80	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
e5d30bc0-b7b9-4f7c-a184-5a48fc5fdb1a	8876bf92-d666-4326-8d0c-46798c8c0c80	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
bedd5097-fd1c-44f7-8955-d726d6212ed3	8876bf92-d666-4326-8d0c-46798c8c0c80	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
9dfbeb37-22fc-46e0-adb1-4bb5a0d60a43	8876bf92-d666-4326-8d0c-46798c8c0c80	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
8365d36e-dcc2-42c3-b4b2-176e396d276e	9d5ed659-f519-4ddc-be8a-dcac42a97f3b	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
2c61e3b1-49dd-4b7e-82bd-e5710e1378e7	9d5ed659-f519-4ddc-be8a-dcac42a97f3b	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
560dfa49-7e89-4f95-a899-ba2cb2fadcfe	9d5ed659-f519-4ddc-be8a-dcac42a97f3b	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
18ae5bbb-3916-4d0c-8098-073d4ae22392	9d5ed659-f519-4ddc-be8a-dcac42a97f3b	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
9843d655-0cd8-4823-b0a5-8a9d057daaad	6a46540e-e6f2-4222-a8a5-8cbaa900dfa0	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
3942528e-91f1-456a-806b-a59bc4cf04c3	6a46540e-e6f2-4222-a8a5-8cbaa900dfa0	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
8f6894ff-df75-40c8-b885-483c549c585d	6a46540e-e6f2-4222-a8a5-8cbaa900dfa0	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
a1333849-6444-4aea-a12b-cbdecdb5af18	6a46540e-e6f2-4222-a8a5-8cbaa900dfa0	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
e471f3c0-7aa4-4002-92dd-9272fcdc8d19	f4aefab9-cda0-4345-b7e7-9c1ed2f9d139	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
b5686b9e-dfd0-431e-9892-79040283db17	f4aefab9-cda0-4345-b7e7-9c1ed2f9d139	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
b8f524ce-66e4-4191-bd04-fef58275a84d	f4aefab9-cda0-4345-b7e7-9c1ed2f9d139	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
56f796a3-571a-494d-9a57-a3942ee79eaa	f4aefab9-cda0-4345-b7e7-9c1ed2f9d139	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
83c0339d-bbe8-48ef-9445-d3f06da248af	1234ac4b-4591-4f47-9e89-d0d34c88bebe	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
f1fa7b74-f7c2-42b4-93f2-596c25b0d301	1234ac4b-4591-4f47-9e89-d0d34c88bebe	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
4e7cf4e2-c535-4c2a-91bf-92f0a7573094	1234ac4b-4591-4f47-9e89-d0d34c88bebe	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
c7a77361-350f-4e98-ba28-93d7563dbf4d	1234ac4b-4591-4f47-9e89-d0d34c88bebe	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
59341476-8943-416c-a0d6-8e8645eae004	3743294d-1254-4fec-b1c1-83fb6cbfc91a	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
3cb88125-853d-4f24-9190-53be802d4e0e	3743294d-1254-4fec-b1c1-83fb6cbfc91a	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
86a83cdb-05f6-4cbb-96a1-4463702ef6d9	3743294d-1254-4fec-b1c1-83fb6cbfc91a	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
3b34772a-9a27-45f6-8bc0-a1c5d57b935c	3743294d-1254-4fec-b1c1-83fb6cbfc91a	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
41125a55-7f35-4daa-b933-d9da7804e946	d2aca80f-1fd0-4089-b019-de7be1a4b4b4	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
5bd07795-fadf-468e-907c-6c22a4cf6733	d2aca80f-1fd0-4089-b019-de7be1a4b4b4	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
5a5c1144-7157-4d81-a64c-f2dcc0aad855	d2aca80f-1fd0-4089-b019-de7be1a4b4b4	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
81916ec1-753b-47bc-96b3-5be2652792d6	d2aca80f-1fd0-4089-b019-de7be1a4b4b4	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
7595f8ba-c1e9-4929-9f02-78def85c8ac8	2938df5b-2b25-4641-9bae-3fb9b2558f9e	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
9e28b9e1-f594-4a0d-ba15-3072e347e5ba	2938df5b-2b25-4641-9bae-3fb9b2558f9e	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
aadf326c-1cee-4321-8a40-69ab329e9db1	2938df5b-2b25-4641-9bae-3fb9b2558f9e	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
dbd2cc2b-4454-4e89-a34a-60110058812d	2938df5b-2b25-4641-9bae-3fb9b2558f9e	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
3f7437e1-6bd7-4387-98b4-be39319584c7	22fdb297-9d4c-46c9-b98d-b246fcc1cdfa	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
0ccbed01-f676-40a7-8bb1-07166cc21f2a	22fdb297-9d4c-46c9-b98d-b246fcc1cdfa	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
4f5ceedd-547e-4bcb-91f8-7bb2922cec52	22fdb297-9d4c-46c9-b98d-b246fcc1cdfa	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
707fadfb-bab1-44e1-923e-9067ae283248	22fdb297-9d4c-46c9-b98d-b246fcc1cdfa	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
9bd25c7f-38f7-4518-9043-b32d09b7f02e	6cf18b05-fd08-4f59-861e-dbbec52be2db	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
2d2a554b-3ff9-40f2-8e28-9d4ecac716d7	6cf18b05-fd08-4f59-861e-dbbec52be2db	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
6287173a-8e9b-4f38-ab9b-30737f2f7234	6cf18b05-fd08-4f59-861e-dbbec52be2db	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
f896124a-03ab-4be8-9518-77dc2619f2f0	6cf18b05-fd08-4f59-861e-dbbec52be2db	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
edfdca5f-80ff-4229-99a0-3bc7b58b19d3	5a2b809d-90b2-459d-a928-baf46b8bd2bf	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
2c34312e-a1d4-41b3-a5e9-3a268339a8b6	5a2b809d-90b2-459d-a928-baf46b8bd2bf	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
a613ca3a-f556-46d2-b666-110c97d2df38	5a2b809d-90b2-459d-a928-baf46b8bd2bf	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
5624d815-2b0a-4b6f-a645-d44d2a528031	5a2b809d-90b2-459d-a928-baf46b8bd2bf	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
9bc48d5d-be44-4fde-a1c6-9bc69ce1252b	51aba369-3e7d-4ab3-9e03-e8719ecb1b6f	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
b963fd6c-7530-43ee-9947-f77aa48e6a98	51aba369-3e7d-4ab3-9e03-e8719ecb1b6f	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
5d0d5f79-6342-4c3f-802d-0b4b5b541c07	51aba369-3e7d-4ab3-9e03-e8719ecb1b6f	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
a5d14e2e-193b-41b9-acdf-12f5713ffb09	51aba369-3e7d-4ab3-9e03-e8719ecb1b6f	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
29aa875d-dff2-4564-9dfe-3dd505b1effa	6f52dbb1-d068-47c4-b9d2-8920b7b80965	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
87428a60-8e8e-4492-9116-d01ff45b4789	6f52dbb1-d068-47c4-b9d2-8920b7b80965	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
140a2f4b-2ede-4a39-a0f1-c06123d4b7cf	6f52dbb1-d068-47c4-b9d2-8920b7b80965	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
8d8f80c2-5b7f-46b2-8b15-b92d480e1324	6f52dbb1-d068-47c4-b9d2-8920b7b80965	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
4622e12d-0d8b-43ca-90a8-b37b3376d64f	bfadd4c5-9887-4e45-bda5-c93237c4c98a	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
81288071-6c70-4f17-a159-3ee6df0299f0	bfadd4c5-9887-4e45-bda5-c93237c4c98a	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
9881894c-e002-42cc-ac87-d2937e651a64	bfadd4c5-9887-4e45-bda5-c93237c4c98a	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
365a6a5d-860d-46a1-a66b-3869008b8110	bfadd4c5-9887-4e45-bda5-c93237c4c98a	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
86f55440-978e-45f2-8df3-e46ae6eeb55d	fcf8e799-3d54-43b9-801c-2f43275b91d2	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
8e75b591-fdd7-4be2-a881-9e601f51fa67	fcf8e799-3d54-43b9-801c-2f43275b91d2	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
ce5a4bff-4bab-4d11-9100-accbc1b4c0e0	fcf8e799-3d54-43b9-801c-2f43275b91d2	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
34fb365c-c0cd-4ffc-9278-518105f41405	fcf8e799-3d54-43b9-801c-2f43275b91d2	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
055f6c1d-710c-46fc-893c-fb1ef067961c	59342e00-b5e5-4628-a721-f3c0c64ed2f0	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
99b6e657-36b2-49a5-8cab-e6762691e5c2	59342e00-b5e5-4628-a721-f3c0c64ed2f0	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
93fef99a-157d-4957-b89d-1f310666f380	59342e00-b5e5-4628-a721-f3c0c64ed2f0	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
b6e450ab-73a7-465e-ad8b-29dd9fd2aeb0	59342e00-b5e5-4628-a721-f3c0c64ed2f0	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
e27e2ef2-2981-45af-b1af-b992ad58abc4	d08555a1-eceb-4318-8ac9-d18b43ecbdc4	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
d18459ec-cd0e-44b6-ac2d-d18926dc53ca	d08555a1-eceb-4318-8ac9-d18b43ecbdc4	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
9cbb2b88-9d8f-440b-9319-77f48221ed06	d08555a1-eceb-4318-8ac9-d18b43ecbdc4	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
f241ce8d-aed5-49d5-894d-f59f2f4a6453	d08555a1-eceb-4318-8ac9-d18b43ecbdc4	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
7eeb18d3-1c3a-456c-8f58-5b50cc3fbf32	d4cf82e5-e21f-498c-835c-4337474350b1	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
d1e3951f-699b-491a-9b21-ace5fae21835	d4cf82e5-e21f-498c-835c-4337474350b1	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
84d0ef13-4164-431a-b82a-dc30c3b6e763	d4cf82e5-e21f-498c-835c-4337474350b1	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
c6c8d794-783e-4d49-9577-5fc043589d23	d4cf82e5-e21f-498c-835c-4337474350b1	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
2ff20a69-4ac9-4f12-9051-cec7531d3709	109f8514-ee3d-4bac-baaf-6a58b08833c4	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
c2adad08-1de1-4591-b732-adbf8244ce45	109f8514-ee3d-4bac-baaf-6a58b08833c4	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
70002585-6ff7-4d3a-8e4e-3ca133eff150	109f8514-ee3d-4bac-baaf-6a58b08833c4	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
d851337c-866a-45fa-b902-94e8f1d3514c	109f8514-ee3d-4bac-baaf-6a58b08833c4	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
599f53b4-6b18-4f98-a296-7cd5bdf0f4f7	b7531faa-ea1b-4a22-a485-6e02ddda01de	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
0697b7ad-6528-4004-9212-b6f119432596	b7531faa-ea1b-4a22-a485-6e02ddda01de	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
bf3e3184-dad1-4994-b491-4e486bed32a7	b7531faa-ea1b-4a22-a485-6e02ddda01de	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
4146fc92-953e-4d77-bed9-813ad0469fe2	b7531faa-ea1b-4a22-a485-6e02ddda01de	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
c3f119c8-f0cf-4d2c-98a9-5e40228c82d9	abd0a872-34f4-431f-a879-5ceb9f973fa9	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
b2f92369-5a3d-45d3-b189-1b7e616c112a	abd0a872-34f4-431f-a879-5ceb9f973fa9	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
b959becc-8a27-411b-9538-ee2e1959e071	abd0a872-34f4-431f-a879-5ceb9f973fa9	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
7c84991e-1130-49c9-bb01-7f78069bfdc2	abd0a872-34f4-431f-a879-5ceb9f973fa9	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
a2aeb693-963f-456d-92de-0d6ce29c2e0f	d2b1e68e-1f0b-421c-b60f-c1c9541cf160	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
26d8eb91-3ddd-4536-a477-0efc5db26d37	d2b1e68e-1f0b-421c-b60f-c1c9541cf160	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
ecd93fac-287c-4b8b-9bc8-13ec8dcda072	d2b1e68e-1f0b-421c-b60f-c1c9541cf160	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
f4356aa9-687b-462b-859c-e5750ea543f4	d2b1e68e-1f0b-421c-b60f-c1c9541cf160	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
b0d47a8c-f2b1-4da8-92fd-db0b843833ef	f1a10000-0000-0000-0000-000000000001	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
e829c548-c94b-4995-ba4b-7ac0be41232d	f1a10000-0000-0000-0000-000000000001	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
627d1f5a-d9c9-4c95-8caa-11df51af4b1b	f1a10000-0000-0000-0000-000000000001	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
df4f1232-5e1d-42b4-88ed-1c43b6bf0697	f1a10000-0000-0000-0000-000000000002	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
3009ea5f-fa92-4bcc-9f7e-7f5aa64d767b	f1a10000-0000-0000-0000-000000000002	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
955c5037-0205-4001-bd1f-40208508c742	f1a10000-0000-0000-0000-000000000002	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
f52e0352-6f52-41b6-afee-e1848d33277b	f1a10000-0000-0000-0000-000000000002	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
f9c2dae4-6e9d-47e3-a639-0a5466c72b74	f1a10000-0000-0000-0000-000000000003	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
d767a082-0f6b-4b86-a2db-0c7bd67c66cd	f1a10000-0000-0000-0000-000000000003	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
f4637bad-8d96-4495-8267-468e72076d5e	f1a10000-0000-0000-0000-000000000003	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
8ca96ac0-593d-46ad-8923-ab8cc710b8b7	f1a10000-0000-0000-0000-000000000003	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
d9c18d5c-89ea-4421-9ade-26e2ae588039	f1a10000-0000-0000-0000-000000000004	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
4fab38d0-2bf2-4a15-93ec-e3e8d0e567ee	f1a10000-0000-0000-0000-000000000004	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
0b73f34d-1b7e-4803-aa84-c07da0744714	f1a10000-0000-0000-0000-000000000004	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
4c12b503-0915-4980-84a1-69e6d205d82f	f1a10000-0000-0000-0000-000000000005	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
30a01cbb-a11d-4c8e-bb0f-6c66c24a9112	f1a10000-0000-0000-0000-000000000005	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
405f7fe0-88bd-4c99-9175-622851ea3dad	f1a10000-0000-0000-0000-000000000005	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
9970ab81-4c30-493b-9c0a-43219b1bf9c0	f1a10000-0000-0000-0000-000000000005	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
e1ddebea-f172-4fe0-9adb-bd7d376c06ab	f1a10000-0000-0000-0000-000000000006	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
c6c1a91d-6377-406c-8e70-fda47d9b084d	f1a10000-0000-0000-0000-000000000006	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
d9a95eb4-df7a-495e-8406-e9642f156205	f1a10000-0000-0000-0000-000000000006	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
32ce1207-e70f-4d2f-816f-6383d719460e	f1a10000-0000-0000-0000-000000000006	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
a2f6c3cd-bbf1-4511-8d56-d74a35709606	f1a10000-0000-0000-0000-000000000007	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
705a1cf3-77de-4e77-adb1-8ad6fb5629d7	f1a10000-0000-0000-0000-000000000007	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
47c608de-1f3d-4fe1-b4bd-e961ab0afd38	f1a10000-0000-0000-0000-000000000007	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
23e069bc-4aa6-4799-9faf-eff43c2fc246	f1a10000-0000-0000-0000-000000000008	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
8accc6dd-ae20-42cd-bd81-2c8863469f11	f1a10000-0000-0000-0000-000000000008	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
da216458-ccfc-4c79-9fa0-db39db7705df	f1a10000-0000-0000-0000-000000000008	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
4ed18183-a620-41db-9f3a-168072be633e	f1a10000-0000-0000-0000-000000000008	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
e3077059-eb2e-4d23-84c0-3102500c1258	f1a10000-0000-0000-0000-000000000009	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
2027c3d6-fece-488a-afa6-8c9f9128f6ab	f1a10000-0000-0000-0000-000000000009	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
4a56d3f1-1084-4fe0-8d66-1f079c10c3c7	f1a10000-0000-0000-0000-000000000009	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
f7d31f60-cb99-4dc4-bab9-449f097ed7dd	f1a10000-0000-0000-0000-000000000009	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
2948c21c-078e-48b1-9032-47e0798649f4	f1a10000-0000-0000-0000-000000000010	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
d9fde095-b025-4b9e-b893-050acc50c117	f1a10000-0000-0000-0000-000000000010	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
fc751a99-3bd8-4ca3-bac4-f68246c6da88	f1a10000-0000-0000-0000-000000000010	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
18abf915-f3b5-4c3c-a42d-28be0299c244	f1a10000-0000-0000-0000-000000000010	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
8a9dcf00-6be6-4d87-b224-0340186dc769	f1a10000-0000-0000-0000-000000000011	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
cf97ea95-d9b1-4744-835d-7b628787ce7b	f1a10000-0000-0000-0000-000000000011	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
26f17d0b-a848-4761-9656-d505e33da071	f1a10000-0000-0000-0000-000000000011	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
982dde54-78da-4431-8470-bc73e9ff9e3e	f1a10000-0000-0000-0000-000000000011	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
9d7a5331-3dca-4aa6-8698-7116704a70f9	4d3127f1-6d25-dc72-9a55-539a943e9357	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
52048144-d589-4f24-8857-11378f70db30	4d3127f1-6d25-dc72-9a55-539a943e9357	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
7c688489-f92c-4da4-adde-8fa2230a1631	4d3127f1-6d25-dc72-9a55-539a943e9357	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
d52d610f-a341-40ce-a495-02ff250f8a15	4d3127f1-6d25-dc72-9a55-539a943e9357	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
db3f7304-7fab-4827-9559-8463a450a635	c75eca9d-c765-8c9b-a8a1-1f3bc3964ec5	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
35a5f51b-2d13-45d8-83fc-19a64c9d1d45	c75eca9d-c765-8c9b-a8a1-1f3bc3964ec5	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
a4ddffef-0a9f-420e-bd33-068d93718122	c75eca9d-c765-8c9b-a8a1-1f3bc3964ec5	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
3e0da4cf-27da-44dd-8d42-28dbd108af73	c75eca9d-c765-8c9b-a8a1-1f3bc3964ec5	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
8c2a9a4a-588b-4829-80a8-1e9edef8d9f7	4f4e8204-7c05-1342-b4ef-dd442b4002d6	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
16cf4b16-3f43-4420-a4a2-7e0cfc9c458a	4f4e8204-7c05-1342-b4ef-dd442b4002d6	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
a0b2c106-40a7-4233-8d38-f9b71f60c7f5	4f4e8204-7c05-1342-b4ef-dd442b4002d6	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
863fa8f4-385d-4e70-834f-0f56c8cb06bf	4f4e8204-7c05-1342-b4ef-dd442b4002d6	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
a73f7bf2-0f01-45a6-9e83-949c4460d458	bd37ab8a-2f2d-f0f7-2658-518de74c04a7	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
c42a7ee4-6dba-4eeb-83fb-cf9fab49d1ac	bd37ab8a-2f2d-f0f7-2658-518de74c04a7	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
36ccbc49-4fcd-4772-b8fc-a813ea5b5440	bd37ab8a-2f2d-f0f7-2658-518de74c04a7	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
83eeeca9-892e-444a-8a6f-bce1d7dc724a	bd37ab8a-2f2d-f0f7-2658-518de74c04a7	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
8fa09adf-4d43-4579-8f0f-c37439bc4059	b5581b88-065b-1d5a-17da-5d12e8d1f6b0	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
c847dca6-060b-413c-bca7-7376a9ebb20d	b5581b88-065b-1d5a-17da-5d12e8d1f6b0	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
34c097bf-1e8b-46c8-989f-db0ff87c29a3	b5581b88-065b-1d5a-17da-5d12e8d1f6b0	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
a15b8b4d-1a21-4bd6-ae1c-7498d1cc032a	b5581b88-065b-1d5a-17da-5d12e8d1f6b0	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
13b5ab50-7195-4f81-bf9c-4decdbb5d6bc	cfe63aa7-0c86-1602-0853-7d68cfacf273	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
a9a195fc-e7ff-4539-988a-c1667ee3879c	cfe63aa7-0c86-1602-0853-7d68cfacf273	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
e156cbab-bb0e-4bcb-9a97-fdf9c61d112f	cfe63aa7-0c86-1602-0853-7d68cfacf273	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
b6480458-9b7c-447c-adb0-64b1b3a7e415	cfe63aa7-0c86-1602-0853-7d68cfacf273	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
4a277d48-7a16-471c-854b-c16ed92dbc05	a7f52140-1e44-e3f2-ac00-fd0dabea90f7	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
8f2ef274-511a-41d3-9533-213a2cc54cd9	a7f52140-1e44-e3f2-ac00-fd0dabea90f7	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
48a7362b-06c1-4c00-9528-37256484d3c9	a7f52140-1e44-e3f2-ac00-fd0dabea90f7	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
13478552-36ea-4f73-a156-2f2c972d174a	a7f52140-1e44-e3f2-ac00-fd0dabea90f7	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
06626fa6-b357-4bbf-9b9c-92c5b6b6c22c	eb693ae8-0397-b21c-584a-23eb12387f15	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
536b6e9a-219d-42e2-aff1-78d0c9cf7234	eb693ae8-0397-b21c-584a-23eb12387f15	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
e56ceca2-fae5-45c4-970e-9bc4b5857b90	eb693ae8-0397-b21c-584a-23eb12387f15	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
aed8faca-7d30-4b55-a54a-7e20b0801607	eb693ae8-0397-b21c-584a-23eb12387f15	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
83215eb5-cb55-4d7c-b625-bd563bb23e60	cd625ac5-40bc-35b6-bd22-c73b1b727e32	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
84160b33-4fa4-4e3a-94da-6a773070cb42	cd625ac5-40bc-35b6-bd22-c73b1b727e32	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
a0677e29-853d-4013-b4c2-1e63c80f9f58	cd625ac5-40bc-35b6-bd22-c73b1b727e32	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
1fac8eb6-fdf2-4ee9-a1ed-25d4b9ffe398	cd625ac5-40bc-35b6-bd22-c73b1b727e32	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
528f1b77-55ed-4ee1-8692-b8c80181fb86	ed25e8c1-df17-252e-3c91-0fb53cee507f	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
7956c72a-c475-4075-a903-0c5e5bac875b	ed25e8c1-df17-252e-3c91-0fb53cee507f	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
2ee579d9-0b49-4f43-8897-1e0f71231ff6	ed25e8c1-df17-252e-3c91-0fb53cee507f	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
8f6ea57c-5760-4693-9b7b-aad1b2ee90c4	ed25e8c1-df17-252e-3c91-0fb53cee507f	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
70d7dbd7-dd2b-4c38-ae3e-d0a2159c5d1e	53035481-d05f-80c8-93de-e25c334b740e	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
c8da6009-b52d-4bb8-8e17-0935afaa22a0	53035481-d05f-80c8-93de-e25c334b740e	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
f4f0b774-e57d-4d3b-a3eb-69e9bf37fc2b	53035481-d05f-80c8-93de-e25c334b740e	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
4691490e-88b5-4ae6-8cb0-71bf5d140f05	53035481-d05f-80c8-93de-e25c334b740e	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
5d8c6d71-56ab-44b0-bf48-1eea9ca09a5d	6222e633-cbd5-46a2-3e5e-642e444ab6ad	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
5b7246e6-ddc9-47bf-bdbc-6053fe07d91e	6222e633-cbd5-46a2-3e5e-642e444ab6ad	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
54f857ee-9a95-4cbc-887e-72b7e24fef98	6222e633-cbd5-46a2-3e5e-642e444ab6ad	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
2b7e8adf-54e3-452f-80f4-c20687f807ca	6222e633-cbd5-46a2-3e5e-642e444ab6ad	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
03bfe96a-7fc4-4ab3-b227-75af708a8d66	c500c659-929e-eb77-2ac2-ed31eada287a	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
3046f60f-c1e5-4fde-9767-70b6a653c2e0	c500c659-929e-eb77-2ac2-ed31eada287a	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
e6e972ab-1a38-4793-9a6d-95135785c2c5	c500c659-929e-eb77-2ac2-ed31eada287a	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
51121eae-f7c4-4ce9-b425-236b5ad0bd5c	c500c659-929e-eb77-2ac2-ed31eada287a	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
7717611a-cadd-434a-88e3-92ff2856ddf4	2ce3eb86-46b2-32dc-851c-cf9ee3435fed	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
090c2630-4ea2-4627-8759-ed894955e45f	2ce3eb86-46b2-32dc-851c-cf9ee3435fed	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
0b9e8cde-2808-486e-973b-d20109569c31	2ce3eb86-46b2-32dc-851c-cf9ee3435fed	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
282568b7-065c-48d5-9524-11987dab0938	2ce3eb86-46b2-32dc-851c-cf9ee3435fed	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
7c6c8820-b89f-4977-b627-fc6673b20fb4	32ca0ec0-b7f2-cd10-d8f6-e86de458cce1	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
021bb491-7348-42d8-b6bd-2784645f9768	32ca0ec0-b7f2-cd10-d8f6-e86de458cce1	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
08ba3c65-03da-4c9a-a9d9-9cd98b4ef0aa	32ca0ec0-b7f2-cd10-d8f6-e86de458cce1	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
0825cb7c-c5f8-4dc7-82a0-e4fe8a811797	32ca0ec0-b7f2-cd10-d8f6-e86de458cce1	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
adbfbd0c-037a-4998-b664-289e9d9ddbcf	697ee990-2e0c-630f-ddaa-2f14e3dfd7c6	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
bcdd4acf-df37-44fe-8fc6-96a13f07dab7	697ee990-2e0c-630f-ddaa-2f14e3dfd7c6	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
34c8aa4b-2141-4a66-ac29-b84778e57493	697ee990-2e0c-630f-ddaa-2f14e3dfd7c6	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
5c32479c-9e6c-4986-b91e-4fa8063a150b	697ee990-2e0c-630f-ddaa-2f14e3dfd7c6	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
58015c26-c3c3-4932-9d3c-61b08248b90d	355fce8c-6d4f-ceff-2004-a3d47e8790e9	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
89751228-8741-44b9-acc7-1ab9b58254d1	355fce8c-6d4f-ceff-2004-a3d47e8790e9	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
3e503c12-0392-4869-b782-0a761d2e57c3	355fce8c-6d4f-ceff-2004-a3d47e8790e9	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
ba1b17fc-b28e-4c9a-bf67-89b15d477dfd	355fce8c-6d4f-ceff-2004-a3d47e8790e9	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
b1da5de7-7a2e-4fa9-9fea-a648c7d2d2d2	b0f5845f-1e2c-ee33-3ccc-e3d3bbdc2218	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
93989715-c47b-4999-8b19-2309cf5a9ded	b0f5845f-1e2c-ee33-3ccc-e3d3bbdc2218	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
f8f3bbe9-c0a9-494b-a19c-a9c53f5b7ec9	b0f5845f-1e2c-ee33-3ccc-e3d3bbdc2218	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
9e35491a-492a-4d45-86d8-c73ee9ca0cb0	b0f5845f-1e2c-ee33-3ccc-e3d3bbdc2218	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
0eb7547e-442f-4603-906f-6824500254c4	5262364a-0e33-55fb-d33a-654f6b13fe64	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
b0546f6b-7a75-4a9a-bba2-a9d9cc6f94f3	5262364a-0e33-55fb-d33a-654f6b13fe64	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
e5d8e89f-a0c8-4329-ab44-e4b3ece26b40	5262364a-0e33-55fb-d33a-654f6b13fe64	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
40eee337-5e45-4584-9ccc-9a98a7c0201a	5262364a-0e33-55fb-d33a-654f6b13fe64	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
d2860ff1-62d5-424d-a94b-7672f85c12af	747089d8-260a-475a-974d-e94b4b0c8bd5	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
8605e4f4-6ada-487a-807b-46141ad02a2f	747089d8-260a-475a-974d-e94b4b0c8bd5	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
6ff1b599-9f1f-4bf1-b3da-9f76aec2b195	747089d8-260a-475a-974d-e94b4b0c8bd5	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
1442fece-90e2-4207-a62e-ab2c302d9f00	747089d8-260a-475a-974d-e94b4b0c8bd5	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
79ff58ab-3540-4398-a595-7e753b220375	97c7617d-7de5-3211-c346-7e1193ca596e	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
517a7038-7094-449d-8716-ecafe2cc34e9	97c7617d-7de5-3211-c346-7e1193ca596e	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
39d55cae-73ef-49fd-ab8d-5163934f81ed	97c7617d-7de5-3211-c346-7e1193ca596e	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
40af386b-d742-434c-a687-1aadc5df7ba4	97c7617d-7de5-3211-c346-7e1193ca596e	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
e47c0a84-fea4-4df0-bbe2-ae249801f427	7954becf-2e69-f20a-fb6d-56c35639fbc7	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
6153f331-df23-42c1-98ef-6068c2488a2b	7954becf-2e69-f20a-fb6d-56c35639fbc7	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
35639d78-cf38-459f-a6df-8766d07f4360	7954becf-2e69-f20a-fb6d-56c35639fbc7	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
c75e4693-d030-4d81-ae34-80a28fdd1cef	7954becf-2e69-f20a-fb6d-56c35639fbc7	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
9ed264f1-0c4d-4f38-9319-67f8ccc6a9ca	f716eee7-5246-a987-9fe3-3d10f101b0df	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
db8d5a69-7fef-4163-9c19-86c1b9aacfb6	f716eee7-5246-a987-9fe3-3d10f101b0df	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
012d10d1-e0da-4bc5-a2f4-9f8703c4cf73	f716eee7-5246-a987-9fe3-3d10f101b0df	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
dccafc39-a39d-40e7-b6d9-eee33ba2828d	f716eee7-5246-a987-9fe3-3d10f101b0df	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
28496a04-538a-4f31-80aa-774ab6cf0dee	09734a06-8694-01c5-e1ff-1858630851e8	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
679014ad-f6e4-488a-bf4a-1dc19a94bf98	09734a06-8694-01c5-e1ff-1858630851e8	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
71843f3b-df2b-4d19-88e1-907be78a6cdb	09734a06-8694-01c5-e1ff-1858630851e8	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
e44ee8f8-3dc9-4c70-b5ab-5e912182abd0	09734a06-8694-01c5-e1ff-1858630851e8	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
9fed8a6c-a98e-4c1a-aeee-9e367424555a	3e10d46f-d2a4-4be4-bac6-9d26cb97d53f	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
5084c7a8-a7b1-4020-8f1d-c27b1bf0583b	3e10d46f-d2a4-4be4-bac6-9d26cb97d53f	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
3079ad7f-4773-4e8b-b96f-aab570f5bd84	3e10d46f-d2a4-4be4-bac6-9d26cb97d53f	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
4bb19d0b-4544-4912-996c-37bfbf756705	3e10d46f-d2a4-4be4-bac6-9d26cb97d53f	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
dda59870-4b64-4d37-b51e-4b3d630573e1	4dd9dad8-d523-6cd0-9149-ef2cf6240e83	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
fb4e8820-f735-43d0-b97e-95856f9e8011	4dd9dad8-d523-6cd0-9149-ef2cf6240e83	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
8a1230b3-3c75-4d58-9fcd-bee2deec75e2	4dd9dad8-d523-6cd0-9149-ef2cf6240e83	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
e84052bf-af31-4edc-9c94-1d71fb943e7a	4dd9dad8-d523-6cd0-9149-ef2cf6240e83	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
d7baeae5-c287-4328-ab5c-673a8c2d7495	3de7b3db-9007-a36f-a9eb-96afc53b4e65	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
b4242baf-12a3-439a-b4ec-4d71e7356123	3de7b3db-9007-a36f-a9eb-96afc53b4e65	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
138c14d1-57eb-4567-80fc-46d5635e8eda	3de7b3db-9007-a36f-a9eb-96afc53b4e65	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
49e7de3d-5563-40c5-9b06-fd094d02e6ed	3de7b3db-9007-a36f-a9eb-96afc53b4e65	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
df1a134c-099e-4d23-841a-9ccffccb6199	32e3c82f-071a-c506-f382-c74e443b9cf2	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
477e49e5-a5aa-4799-91e5-c9913eac8cec	32e3c82f-071a-c506-f382-c74e443b9cf2	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
0f8e28d6-1585-4d32-8a2b-5ddd155d8ce6	32e3c82f-071a-c506-f382-c74e443b9cf2	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
749bfa26-fdd2-48a2-b908-eab5af802221	32e3c82f-071a-c506-f382-c74e443b9cf2	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
f70a6bb1-df29-47bf-a1e1-1107139c5c22	0954d883-2ff4-8a36-1af7-e68b508fb8a6	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
6ba9e078-d6fb-444b-97d3-b815cd7b087c	0954d883-2ff4-8a36-1af7-e68b508fb8a6	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
ac7b473e-6abd-4d39-a20c-3f3e5ec75370	0954d883-2ff4-8a36-1af7-e68b508fb8a6	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
6a4dc5a4-4393-4420-bc5d-a402bcbcf806	0954d883-2ff4-8a36-1af7-e68b508fb8a6	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
4c3d6059-9ae0-4de7-9ebc-0e091f33b379	765e9e28-be11-a736-4720-b2f2bc8dc33f	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
1b51ed69-b342-482f-a084-61f992c40518	765e9e28-be11-a736-4720-b2f2bc8dc33f	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
21dab525-9672-4898-b643-9f9d05595618	765e9e28-be11-a736-4720-b2f2bc8dc33f	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
2a136078-dba6-4f37-9822-5141c6fd5792	765e9e28-be11-a736-4720-b2f2bc8dc33f	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
81a388a2-15e7-4d3b-aeca-2bbe89a5234d	bf67b89d-9ed4-d73c-07d5-b37f8d6a9de6	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
bf168fc4-9bc6-4cf4-8613-3ad9cf500437	bf67b89d-9ed4-d73c-07d5-b37f8d6a9de6	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
40873a68-77c7-40c5-bf0f-6b53584520ff	bf67b89d-9ed4-d73c-07d5-b37f8d6a9de6	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
c6bc868d-76bc-4150-bb5c-1dad38b01921	bf67b89d-9ed4-d73c-07d5-b37f8d6a9de6	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
64c5812e-f99b-44a0-8c44-eeab3f12e2ac	6c883e87-4c86-2861-e5fd-0064aefdec18	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
396d8bb2-cf9e-490d-9eda-39a41cb1d182	6c883e87-4c86-2861-e5fd-0064aefdec18	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
74bc7167-487c-4c6e-a4a4-1934001de64e	6c883e87-4c86-2861-e5fd-0064aefdec18	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
63900ee9-61fd-44e2-8374-92952eb26a2c	6c883e87-4c86-2861-e5fd-0064aefdec18	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
c8c74f3b-4093-44bd-a00d-1c858e09616b	f29268e9-7f58-36d4-bb45-874297be6cf5	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
01be938f-dcda-477f-9dc6-3563dc9ed47a	f29268e9-7f58-36d4-bb45-874297be6cf5	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
8dee651e-b783-4587-8bdd-e3f50f13a165	f29268e9-7f58-36d4-bb45-874297be6cf5	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
a140f3a9-6121-4336-9e72-9d550d8d32e7	f29268e9-7f58-36d4-bb45-874297be6cf5	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
5c118fe2-18fb-44d7-8ff8-7881c569f05f	7a16db7f-8372-bdda-d3a5-93fbab2ec3b3	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
dc25a160-ba48-4ccb-9bff-7ec76371b131	7a16db7f-8372-bdda-d3a5-93fbab2ec3b3	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
41b7a0b9-43c1-4b23-95f9-d6ce593f81af	7a16db7f-8372-bdda-d3a5-93fbab2ec3b3	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
fa664f98-d85c-4290-93a7-cff8999814b3	7a16db7f-8372-bdda-d3a5-93fbab2ec3b3	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
afcc997a-6508-4999-80e9-1555576af515	a2da06de-1b56-c01b-d1fa-d31f32e27094	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
8ef626d7-e235-4202-8f3e-6d2d07166461	a2da06de-1b56-c01b-d1fa-d31f32e27094	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
e30d9772-c190-4b0c-a8ae-6a38141dcc18	a2da06de-1b56-c01b-d1fa-d31f32e27094	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
23ddfdfd-8ee2-4807-bd6e-0aca45c4ae25	a2da06de-1b56-c01b-d1fa-d31f32e27094	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
307c1a17-1113-4e9c-8e31-16d6183140d8	4cbaf7fc-023c-28c7-3d83-75526b9d5eac	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
8ef20607-be27-45be-957f-f6ac22348075	4cbaf7fc-023c-28c7-3d83-75526b9d5eac	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
4fa47308-2780-4e66-977b-0acc380cd4bd	4cbaf7fc-023c-28c7-3d83-75526b9d5eac	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
f26a5165-e2d9-4796-a258-af8a6c7787c0	4cbaf7fc-023c-28c7-3d83-75526b9d5eac	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
645968e1-063e-43f1-9249-4304a7538037	5e4a5eda-cf31-a444-7964-852c603a9ca1	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
7a54605a-1139-4a59-a83e-cccc6ea444e3	5e4a5eda-cf31-a444-7964-852c603a9ca1	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
2289d06e-4c15-435b-9dc1-eee3e78a9c60	5e4a5eda-cf31-a444-7964-852c603a9ca1	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
895ba5cc-ce36-44b2-84f3-4b24f441d52b	5e4a5eda-cf31-a444-7964-852c603a9ca1	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
5d31c2f8-9f61-4516-96ce-d43cae96fe5d	b0cc2882-be85-f28b-4188-ffc6fc00344a	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
4298a96f-7dce-414d-b18f-2c0a25bc66c4	b0cc2882-be85-f28b-4188-ffc6fc00344a	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
04073e45-428c-4f5c-a154-6fc526a9eb64	b0cc2882-be85-f28b-4188-ffc6fc00344a	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
8ba019db-0905-4725-b73b-f2b24bf579b8	b0cc2882-be85-f28b-4188-ffc6fc00344a	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
dcee80b1-45b0-4162-ac06-4bb453327cc4	174c3c7e-7316-5cb1-4d19-e3b6fa3168f8	https://picsum.photos/seed/bookshare1/600/800	2026-10-15 05:52:10.135342
977ffc29-bcf9-48dc-a4f5-a74cc29e0dfa	174c3c7e-7316-5cb1-4d19-e3b6fa3168f8	https://picsum.photos/seed/bookshare2/600/800	2026-10-15 05:52:10.135342
fe751f27-a1a6-44e6-8aa4-e2564056faaa	174c3c7e-7316-5cb1-4d19-e3b6fa3168f8	https://picsum.photos/seed/bookshare3/600/800	2026-10-15 05:52:10.135342
0625dc1e-4429-4e54-a75e-bf7ea2c47151	174c3c7e-7316-5cb1-4d19-e3b6fa3168f8	https://picsum.photos/seed/bookshare4/600/800	2026-10-15 05:52:10.135342
\.


--
-- Data for Name: listings; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.listings (id, owner_id, book_catalog_entry_id, condition_description, settlement_type, region, settlement_name, delivery_methods, status, created_at, updated_at, hidden_at, hidden_reason, hidden_by) FROM stdin;
b2222222-0000-0000-0000-000000000001	11111111-1111-1111-1111-111111111111	a1111111-0000-0000-0000-000000000002	Легкі потертості на обкладинці, всередині чисто.	CITY	Київська область	Київ	{PICKUP,MAIL}	IN_EXCHANGE	2026-09-05 05:43:14.070375	2026-10-05 05:43:14.070375	\N	\N	\N
b2222222-0000-0000-0000-000000000002	22222222-2222-2222-2222-222222222222	a1111111-0000-0000-0000-000000000001	Стан майже новий.	CITY	Львівська область	Львів	{PICKUP}	AVAILABLE	2026-09-15 05:43:14.070375	2026-10-05 05:43:14.070375	\N	\N	\N
b2222222-0000-0000-0000-000000000003	44444444-4444-4444-4444-444444444444	a1111111-0000-0000-0000-000000000004	Читана один раз, без поміток.	CITY	Харківська область	Харків	{PICKUP,MAIL}	AVAILABLE	2026-09-25 05:43:14.070375	2026-10-05 05:43:14.070375	\N	\N	\N
b2222222-0000-0000-0000-000000000004	33333333-3333-3333-3333-333333333333	a1111111-0000-0000-0000-000000000003	Є підкреслення олівцем на кількох сторінках.	VILLAGE	Одеська область	Затока	{MAIL}	RESERVED	2026-09-30 05:43:14.070375	2026-10-05 05:43:14.070375	\N	\N	\N
401add04-e443-41e7-9800-75810398a42d	44444444-4444-4444-4444-444444444444	da717bbc-9ed8-42fb-96b9-2f4f3415c619	Гарний стан, читалась акуратно	CITY	Харківська область	Харків	{PICKUP,MAIL}	AVAILABLE	2026-09-17 10:05:10.263204	2026-10-05 05:43:14.67561	\N	\N	\N
8cf403d3-1075-4361-87e3-f95b4fa56bb8	44444444-4444-4444-4444-444444444444	9256f772-9822-4940-982c-c74fc6a00ab1	Гарний стан, читалась акуратно	CITY	Харківська область	Харків	{PICKUP,MAIL}	AVAILABLE	2026-09-19 15:35:17.97887	2026-10-05 05:43:14.67561	\N	\N	\N
699ee919-6aac-4f3f-9d3d-53a6e0483c9f	44444444-4444-4444-4444-444444444444	b5c5b2e0-56a9-46ba-ad59-b8b0bd4d4c8f	Гарний стан, читалась акуратно	CITY	Харківська область	Харків	{PICKUP,MAIL}	AVAILABLE	2026-09-21 12:13:56.024573	2026-10-05 05:43:14.67561	\N	\N	\N
490cd468-99f2-49cf-a63f-eed79e41da04	44444444-4444-4444-4444-444444444444	496a751c-2f5d-43ea-b44a-c8df7c9f6efa	Гарний стан, читалась акуратно	CITY	Харківська область	Харків	{PICKUP,MAIL}	AVAILABLE	2026-10-02 18:59:37.907387	2026-10-05 05:43:14.67561	\N	\N	\N
73ae542a-a1cf-4378-ac97-08f8ae8315a1	44444444-4444-4444-4444-444444444444	f2d52c59-26e1-482f-887d-c50fce53adc8	Гарний стан, читалась акуратно	CITY	Харківська область	Харків	{PICKUP,MAIL}	AVAILABLE	2026-09-17 05:05:19.864335	2026-10-05 05:43:14.67561	\N	\N	\N
ed0e657f-4656-4567-83a7-56ca47a4d00a	44444444-4444-4444-4444-444444444444	66789058-6fa0-4ae1-8861-8ccf2055fbd4	Гарний стан, читалась акуратно	CITY	Харківська область	Харків	{PICKUP,MAIL}	AVAILABLE	2026-09-14 10:29:57.238628	2026-10-05 05:43:14.67561	\N	\N	\N
f2e8e325-d074-4967-901f-4cf9b0a0ffe9	44444444-4444-4444-4444-444444444444	ec10989f-c393-4909-9dc8-1694234cd286	Гарний стан, читалась акуратно	CITY	Харківська область	Харків	{PICKUP,MAIL}	AVAILABLE	2026-09-18 21:46:18.689554	2026-10-05 05:43:14.67561	\N	\N	\N
c640c4a5-07de-41f6-8777-62565392c8cb	44444444-4444-4444-4444-444444444444	1b670d23-6146-4517-871a-e755d24c8bc8	Гарний стан, читалась акуратно	CITY	Харківська область	Харків	{PICKUP,MAIL}	AVAILABLE	2026-09-25 21:44:45.197228	2026-10-05 05:43:14.67561	\N	\N	\N
dfd39ae4-55a2-4892-a8ec-1fbfa4ea5f1a	44444444-4444-4444-4444-444444444444	9537744f-baea-4de8-9e93-c1c46d9c72a0	Гарний стан, читалась акуратно	CITY	Харківська область	Харків	{PICKUP,MAIL}	AVAILABLE	2026-09-11 00:23:26.526466	2026-10-05 05:43:14.67561	\N	\N	\N
7467a0ed-e101-43c6-bd1e-b1e9e03f0d2d	44444444-4444-4444-4444-444444444444	13c74ae5-5062-4750-9a34-4f696be53a74	Гарний стан, читалась акуратно	CITY	Харківська область	Харків	{PICKUP,MAIL}	AVAILABLE	2026-09-17 11:48:33.647885	2026-10-05 05:43:14.67561	\N	\N	\N
69727724-d0b7-4a18-9ce0-4076b0f3afa9	44444444-4444-4444-4444-444444444444	1ed309d0-23c8-414e-b0e1-aab71c56a482	Гарний стан, читалась акуратно	CITY	Харківська область	Харків	{PICKUP,MAIL}	AVAILABLE	2026-09-14 23:26:55.333008	2026-10-05 05:43:14.67561	\N	\N	\N
a3a3e9de-bffa-4ed5-8e96-22acfe58cf2d	44444444-4444-4444-4444-444444444444	80d9cb61-ce54-4db9-97b4-d64e3703ea94	Гарний стан, читалась акуратно	CITY	Харківська область	Харків	{PICKUP,MAIL}	AVAILABLE	2026-09-28 15:35:42.990975	2026-10-05 05:43:14.67561	\N	\N	\N
18abb25c-4c5b-4b4f-9c8f-332c79f0fbac	44444444-4444-4444-4444-444444444444	c6ae0362-6860-42da-a181-1b8128cbdbd6	Гарний стан, читалась акуратно	CITY	Харківська область	Харків	{PICKUP,MAIL}	AVAILABLE	2026-09-25 22:48:55.434021	2026-10-05 05:43:14.67561	\N	\N	\N
a424e05a-b417-4118-bd21-143cdb9da0bb	44444444-4444-4444-4444-444444444444	21052470-8496-45cb-90da-dc6f262ef9c4	Гарний стан, читалась акуратно	CITY	Харківська область	Харків	{PICKUP,MAIL}	AVAILABLE	2026-09-06 18:21:49.590155	2026-10-05 05:43:14.67561	\N	\N	\N
7a808062-2721-4dc1-9ad8-07b9fb0524f1	44444444-4444-4444-4444-444444444444	97a743f8-ddd5-4138-8453-1d87f710b6db	Гарний стан, читалась акуратно	CITY	Харківська область	Харків	{PICKUP,MAIL}	AVAILABLE	2026-09-21 19:37:56.568941	2026-10-05 05:43:14.67561	\N	\N	\N
195a8bab-41fe-4340-ad7e-1b3f6a215b22	44444444-4444-4444-4444-444444444444	762068c0-2ce8-4010-a206-5ef25193e699	Гарний стан, читалась акуратно	CITY	Харківська область	Харків	{PICKUP,MAIL}	AVAILABLE	2026-09-07 02:45:19.55296	2026-10-05 05:43:14.67561	\N	\N	\N
e1e9b9f9-9531-4a05-8689-b52cc88d5e47	44444444-4444-4444-4444-444444444444	273a23e1-42f1-499d-9a3f-4c6f60df6b00	Гарний стан, читалась акуратно	CITY	Харківська область	Харків	{PICKUP,MAIL}	AVAILABLE	2026-09-05 08:03:24.553085	2026-10-05 05:43:14.67561	\N	\N	\N
8ff58015-e670-483f-8897-2fb4b251766d	44444444-4444-4444-4444-444444444444	9238eb19-f568-44c5-84ab-251660f95da8	Гарний стан, читалась акуратно	CITY	Харківська область	Харків	{PICKUP,MAIL}	AVAILABLE	2026-09-14 08:38:12.061403	2026-10-05 05:43:14.67561	\N	\N	\N
d9dfe2e4-4c87-4a3e-838d-e8a5b855a0ca	44444444-4444-4444-4444-444444444444	a564feff-6a38-47c2-9193-0ae3c3d2537d	Гарний стан, читалась акуратно	CITY	Харківська область	Харків	{PICKUP,MAIL}	AVAILABLE	2026-09-18 08:52:10.281442	2026-10-05 05:43:14.67561	\N	\N	\N
66b25739-a872-4299-a3e0-7b257454d1ff	44444444-4444-4444-4444-444444444444	ec2fe2e3-9ec2-4dc8-81a9-c5b4f2dd5894	Гарний стан, читалась акуратно	CITY	Харківська область	Харків	{PICKUP,MAIL}	AVAILABLE	2026-09-25 09:22:06.771284	2026-10-05 05:43:14.67561	\N	\N	\N
209f62e4-a6f2-42ef-847d-65ee172f26de	44444444-4444-4444-4444-444444444444	282352bf-e7c0-4a34-a9fa-a99565c848fa	Гарний стан, читалась акуратно	CITY	Харківська область	Харків	{PICKUP,MAIL}	AVAILABLE	2026-09-05 14:20:11.359372	2026-10-05 05:43:14.67561	\N	\N	\N
3f3f8b6e-025d-450d-acc7-8365049f31ab	44444444-4444-4444-4444-444444444444	901edc2b-5004-4f56-90f5-5e014c1f5e92	Гарний стан, читалась акуратно	CITY	Харківська область	Харків	{PICKUP,MAIL}	AVAILABLE	2026-09-13 07:43:34.96045	2026-10-05 05:43:14.67561	\N	\N	\N
7161cfb9-f28f-4a16-a094-389f27aa8e56	44444444-4444-4444-4444-444444444444	21001890-523d-4768-b6be-07dbc727630a	Гарний стан, читалась акуратно	CITY	Харківська область	Харків	{PICKUP,MAIL}	AVAILABLE	2026-09-08 08:56:33.020634	2026-10-05 05:43:14.67561	\N	\N	\N
cae6e485-adda-472c-809e-f91c8e74c7e2	44444444-4444-4444-4444-444444444444	196e8b9c-d397-40f9-b5ac-25411475b20c	Гарний стан, читалась акуратно	CITY	Харківська область	Харків	{PICKUP,MAIL}	AVAILABLE	2026-09-18 18:51:16.430505	2026-10-05 05:43:14.67561	\N	\N	\N
01d2e712-e0a9-40e5-a86c-9992da33f01c	44444444-4444-4444-4444-444444444444	629788dd-291b-4040-b458-11dac8d0504d	Гарний стан, читалась акуратно	CITY	Харківська область	Харків	{PICKUP,MAIL}	AVAILABLE	2026-09-25 10:00:58.799637	2026-10-05 05:43:14.67561	\N	\N	\N
2ba5567c-1f5b-4ca7-9e59-b9ac30412dd7	44444444-4444-4444-4444-444444444444	4d1b7a1b-1c2b-4203-ad56-0c4660a2ffb0	Гарний стан, читалась акуратно	CITY	Харківська область	Харків	{PICKUP,MAIL}	AVAILABLE	2026-09-15 08:07:57.676791	2026-10-05 05:43:14.67561	\N	\N	\N
18ec9025-e2bf-4363-b985-cb015740e041	44444444-4444-4444-4444-444444444444	1adc869a-0678-4f1e-93ee-8f0ecc0190d3	Гарний стан, читалась акуратно	CITY	Харківська область	Харків	{PICKUP,MAIL}	AVAILABLE	2026-09-06 22:01:22.358032	2026-10-05 05:43:14.67561	\N	\N	\N
286b58dc-ea9b-453e-879a-b6dcbe946bae	44444444-4444-4444-4444-444444444444	e201b2d8-0368-4753-a923-50612047e8cd	Гарний стан, читалась акуратно	CITY	Харківська область	Харків	{PICKUP,MAIL}	AVAILABLE	2026-09-20 03:57:25.583258	2026-10-05 05:43:14.67561	\N	\N	\N
8876bf92-d666-4326-8d0c-46798c8c0c80	44444444-4444-4444-4444-444444444444	e9c1153c-2dca-4332-84be-02e6acb7c626	Гарний стан, читалась акуратно	CITY	Харківська область	Харків	{PICKUP,MAIL}	AVAILABLE	2026-09-19 12:33:40.832033	2026-10-05 05:43:14.67561	\N	\N	\N
9d5ed659-f519-4ddc-be8a-dcac42a97f3b	44444444-4444-4444-4444-444444444444	86caa903-2f60-45fa-bac2-db32c6e46311	Гарний стан, читалась акуратно	CITY	Харківська область	Харків	{PICKUP,MAIL}	AVAILABLE	2026-09-23 20:39:24.960233	2026-10-05 05:43:14.67561	\N	\N	\N
6a46540e-e6f2-4222-a8a5-8cbaa900dfa0	44444444-4444-4444-4444-444444444444	a1ae3063-07df-41c1-b895-42039175a64c	Гарний стан, читалась акуратно	CITY	Харківська область	Харків	{PICKUP,MAIL}	AVAILABLE	2026-09-19 12:27:38.137602	2026-10-05 05:43:14.67561	\N	\N	\N
f4aefab9-cda0-4345-b7e7-9c1ed2f9d139	44444444-4444-4444-4444-444444444444	85866955-e1cb-41c4-b40b-c6efaae584a6	Гарний стан, читалась акуратно	CITY	Харківська область	Харків	{PICKUP,MAIL}	AVAILABLE	2026-09-30 18:11:53.077757	2026-10-05 05:43:14.67561	\N	\N	\N
1234ac4b-4591-4f47-9e89-d0d34c88bebe	44444444-4444-4444-4444-444444444444	edca2a98-9944-40a8-9377-bbe21d46726c	Гарний стан, читалась акуратно	CITY	Харківська область	Харків	{PICKUP,MAIL}	AVAILABLE	2026-09-13 01:11:40.205776	2026-10-05 05:43:14.67561	\N	\N	\N
3743294d-1254-4fec-b1c1-83fb6cbfc91a	44444444-4444-4444-4444-444444444444	58db7795-dc1b-4746-bce9-223e36be87e5	Гарний стан, читалась акуратно	CITY	Харківська область	Харків	{PICKUP,MAIL}	AVAILABLE	2026-09-25 23:13:56.08559	2026-10-05 05:43:14.67561	\N	\N	\N
d2aca80f-1fd0-4089-b019-de7be1a4b4b4	44444444-4444-4444-4444-444444444444	ff6887fd-cd06-4858-886d-2ca4a17df394	Гарний стан, читалась акуратно	CITY	Харківська область	Харків	{PICKUP,MAIL}	AVAILABLE	2026-10-03 16:49:27.977816	2026-10-05 05:43:14.67561	\N	\N	\N
2938df5b-2b25-4641-9bae-3fb9b2558f9e	44444444-4444-4444-4444-444444444444	fd95c0a9-b640-427a-9d4d-0ab35734429e	Гарний стан, читалась акуратно	CITY	Харківська область	Харків	{PICKUP,MAIL}	AVAILABLE	2026-09-17 10:49:39.4768	2026-10-05 05:43:14.67561	\N	\N	\N
22fdb297-9d4c-46c9-b98d-b246fcc1cdfa	44444444-4444-4444-4444-444444444444	2ecadb18-2770-4714-96c5-63d7e7484a0c	Гарний стан, читалась акуратно	CITY	Харківська область	Харків	{PICKUP,MAIL}	AVAILABLE	2026-09-21 17:48:22.480998	2026-10-05 05:43:14.67561	\N	\N	\N
6cf18b05-fd08-4f59-861e-dbbec52be2db	44444444-4444-4444-4444-444444444444	203ba202-c3db-42f8-9fe5-23209f7fc579	Гарний стан, читалась акуратно	CITY	Харківська область	Харків	{PICKUP,MAIL}	AVAILABLE	2026-09-11 14:00:50.500531	2026-10-05 05:43:14.67561	\N	\N	\N
5a2b809d-90b2-459d-a928-baf46b8bd2bf	44444444-4444-4444-4444-444444444444	91c56c21-ac9d-4fca-aa59-af25f2e8e545	Гарний стан, читалась акуратно	CITY	Харківська область	Харків	{PICKUP,MAIL}	AVAILABLE	2026-09-29 12:47:23.599293	2026-10-05 05:43:14.67561	\N	\N	\N
51aba369-3e7d-4ab3-9e03-e8719ecb1b6f	44444444-4444-4444-4444-444444444444	aa8cadcf-4612-4d52-ab55-7da5fba850d8	Гарний стан, читалась акуратно	CITY	Харківська область	Харків	{PICKUP,MAIL}	AVAILABLE	2026-09-22 16:21:01.806951	2026-10-05 05:43:14.67561	\N	\N	\N
6f52dbb1-d068-47c4-b9d2-8920b7b80965	44444444-4444-4444-4444-444444444444	f804e166-5298-4e68-b517-f674fffe74f1	Гарний стан, читалась акуратно	CITY	Харківська область	Харків	{PICKUP,MAIL}	AVAILABLE	2026-10-02 12:10:00.710427	2026-10-05 05:43:14.67561	\N	\N	\N
bfadd4c5-9887-4e45-bda5-c93237c4c98a	44444444-4444-4444-4444-444444444444	ea28ebbb-f5df-4d45-862b-3646f8f6c4f9	Гарний стан, читалась акуратно	CITY	Харківська область	Харків	{PICKUP,MAIL}	AVAILABLE	2026-09-17 18:48:21.63484	2026-10-05 05:43:14.67561	\N	\N	\N
fcf8e799-3d54-43b9-801c-2f43275b91d2	44444444-4444-4444-4444-444444444444	58c1d763-647a-4af3-b2fd-f42c240641f0	Гарний стан, читалась акуратно	CITY	Харківська область	Харків	{PICKUP,MAIL}	AVAILABLE	2026-09-10 10:02:40.830027	2026-10-05 05:43:14.67561	\N	\N	\N
59342e00-b5e5-4628-a721-f3c0c64ed2f0	44444444-4444-4444-4444-444444444444	70c03713-a762-4ecb-8bfc-9c01e3e187bc	Гарний стан, читалась акуратно	CITY	Харківська область	Харків	{PICKUP,MAIL}	AVAILABLE	2026-09-09 02:15:01.709222	2026-10-05 05:43:14.67561	\N	\N	\N
d08555a1-eceb-4318-8ac9-d18b43ecbdc4	44444444-4444-4444-4444-444444444444	6c36b942-61bf-4030-b5e0-fd66c99c82e6	Гарний стан, читалась акуратно	CITY	Харківська область	Харків	{PICKUP,MAIL}	AVAILABLE	2026-09-30 17:54:24.643781	2026-10-05 05:43:14.67561	\N	\N	\N
d4cf82e5-e21f-498c-835c-4337474350b1	44444444-4444-4444-4444-444444444444	61a4d625-7995-4298-a5f2-8661cc5d941c	Гарний стан, читалась акуратно	CITY	Харківська область	Харків	{PICKUP,MAIL}	AVAILABLE	2026-10-05 05:38:09.488455	2026-10-05 05:43:14.67561	\N	\N	\N
109f8514-ee3d-4bac-baaf-6a58b08833c4	44444444-4444-4444-4444-444444444444	950f4959-8d21-404a-924f-d08c83ff8d69	Гарний стан, читалась акуратно	CITY	Харківська область	Харків	{PICKUP,MAIL}	AVAILABLE	2026-09-12 21:26:13.08298	2026-10-05 05:43:14.67561	\N	\N	\N
b7531faa-ea1b-4a22-a485-6e02ddda01de	44444444-4444-4444-4444-444444444444	908d066b-5332-417e-ac48-b9800924eeb2	Гарний стан, читалась акуратно	CITY	Харківська область	Харків	{PICKUP,MAIL}	AVAILABLE	2026-09-30 04:26:48.851389	2026-10-05 05:43:14.67561	\N	\N	\N
abd0a872-34f4-431f-a879-5ceb9f973fa9	44444444-4444-4444-4444-444444444444	fc55274d-8aa6-44da-8983-2b055ef75491	Гарний стан, читалась акуратно	CITY	Харківська область	Харків	{PICKUP,MAIL}	AVAILABLE	2026-09-24 06:36:36.125479	2026-10-05 05:43:14.67561	\N	\N	\N
d2b1e68e-1f0b-421c-b60f-c1c9541cf160	44444444-4444-4444-4444-444444444444	93d0c33e-031a-487c-8622-73155aa4cbe2	Гарний стан, читалась акуратно	CITY	Харківська область	Харків	{PICKUP,MAIL}	AVAILABLE	2026-09-27 03:29:04.812708	2026-10-05 05:43:14.67561	\N	\N	\N
f1a10000-0000-0000-0000-000000000002	4d909f2a-4d9a-43fb-9ba4-273c6270b2a0	da717bbc-9ed8-42fb-96b9-2f4f3415c619	Майже як нова.	\N	\N	\N	{PICKUP}	AVAILABLE	2026-09-28 05:59:24.686192	2026-10-05 05:59:24.686192	\N	\N	\N
f1a10000-0000-0000-0000-000000000003	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	1ed309d0-23c8-414e-b0e1-aab71c56a482	Читана один раз, без поміток.	\N	\N	\N	{PICKUP,MAIL}	AVAILABLE	2026-09-29 05:59:24.686192	2026-10-05 05:59:24.686192	\N	\N	\N
f1a10000-0000-0000-0000-000000000004	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	c6ae0362-6860-42da-a181-1b8128cbdbd6	Готова до передачі.	\N	\N	\N	{PICKUP,MAIL}	RESERVED	2026-10-01 05:59:24.686192	2026-10-05 05:59:24.686192	\N	\N	\N
f1a10000-0000-0000-0000-000000000005	4d909f2a-4d9a-43fb-9ba4-273c6270b2a0	9537744f-baea-4de8-9e93-c1c46d9c72a0	Кілька позначок олівцем.	\N	\N	\N	{PICKUP,MAIL}	IN_EXCHANGE	2026-09-25 05:59:24.686192	2026-10-05 05:59:24.686192	\N	\N	\N
f1a10000-0000-0000-0000-000000000006	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	6c36b942-61bf-4030-b5e0-fd66c99c82e6	Легкі потертості.	\N	\N	\N	{MAIL}	IN_EXCHANGE	2026-09-20 05:59:24.686192	2026-10-05 05:59:24.686192	\N	\N	\N
f1a10000-0000-0000-0000-000000000007	4d909f2a-4d9a-43fb-9ba4-273c6270b2a0	496a751c-2f5d-43ea-b44a-c8df7c9f6efa	Чудовий стан.	\N	\N	\N	{PICKUP,MAIL}	AVAILABLE	2026-09-15 05:59:24.686192	2026-10-05 05:59:24.686192	\N	\N	\N
f1a10000-0000-0000-0000-000000000008	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	93d0c33e-031a-487c-8622-73155aa4cbe2	Прострочене повернення.	\N	\N	\N	{PICKUP}	IN_EXCHANGE	2026-09-10 05:59:24.686192	2026-10-05 05:59:24.686192	\N	\N	\N
f1a10000-0000-0000-0000-000000000009	4d909f2a-4d9a-43fb-9ba4-273c6270b2a0	fc55274d-8aa6-44da-8983-2b055ef75491	Триває спір щодо стану книги.	\N	\N	\N	{MAIL}	IN_EXCHANGE	2026-09-17 05:59:24.686192	2026-10-05 05:59:24.686192	\N	\N	\N
f1a10000-0000-0000-0000-000000000010	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	901edc2b-5004-4f56-90f5-5e014c1f5e92	Гарний стан.	\N	\N	\N	{PICKUP,MAIL}	IN_EXCHANGE	2026-09-26 05:59:24.686192	2026-10-05 05:59:24.686192	\N	\N	\N
f1a10000-0000-0000-0000-000000000011	22222222-2222-2222-2222-222222222222	629788dd-291b-4040-b458-11dac8d0504d	Без пошкоджень.	\N	\N	\N	{PICKUP,MAIL}	AVAILABLE	2026-09-13 05:59:24.686192	2026-10-05 05:59:24.686192	\N	\N	\N
4d3127f1-6d25-dc72-9a55-539a943e9357	11111111-1111-1111-1111-111111111111	a1111111-0000-0000-0000-000000000001	Стан хороший.	\N	\N	\N	{PICKUP,MAIL}	AVAILABLE	2026-08-26 05:59:24.686192	2026-10-05 05:59:24.686192	\N	\N	\N
c75eca9d-c765-8c9b-a8a1-1f3bc3964ec5	22222222-2222-2222-2222-222222222222	a1111111-0000-0000-0000-000000000002	Читана один раз.	\N	\N	\N	{PICKUP}	AVAILABLE	2026-08-28 05:59:24.686192	2026-10-05 05:59:24.686192	\N	\N	\N
4f4e8204-7c05-1342-b4ef-dd442b4002d6	33333333-3333-3333-3333-333333333333	a1111111-0000-0000-0000-000000000003	Є позначки олівцем.	\N	\N	\N	{MAIL}	AVAILABLE	2026-08-30 05:59:24.686192	2026-10-05 05:59:24.686192	\N	\N	\N
bd37ab8a-2f2d-f0f7-2658-518de74c04a7	44444444-4444-4444-4444-444444444444	a1111111-0000-0000-0000-000000000004	Майже нова.	\N	\N	\N	{PICKUP,MAIL}	AVAILABLE	2026-09-01 05:59:24.686192	2026-10-05 05:59:24.686192	\N	\N	\N
b5581b88-065b-1d5a-17da-5d12e8d1f6b0	55555555-5555-5555-5555-555555555555	a1111111-0000-0000-0000-000000000005	Гарний стан.	\N	\N	\N	{PICKUP}	AVAILABLE	2026-09-03 05:59:24.686192	2026-10-05 05:59:24.686192	\N	\N	\N
cfe63aa7-0c86-1602-0853-7d68cfacf273	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	9256f772-9822-4940-982c-c74fc6a00ab1	Легкі потертості.	\N	\N	\N	{PICKUP,MAIL}	AVAILABLE	2026-09-05 05:59:24.686192	2026-10-05 05:59:24.686192	\N	\N	\N
a7f52140-1e44-e3f2-ac00-fd0dabea90f7	4d909f2a-4d9a-43fb-9ba4-273c6270b2a0	b5c5b2e0-56a9-46ba-ad59-b8b0bd4d4c8f	Чудовий стан.	\N	\N	\N	{PICKUP}	AVAILABLE	2026-09-07 05:59:24.686192	2026-10-05 05:59:24.686192	\N	\N	\N
eb693ae8-0397-b21c-584a-23eb12387f15	11111111-1111-1111-1111-111111111111	f2d52c59-26e1-482f-887d-c50fce53adc8	Без пошкоджень.	\N	\N	\N	{MAIL}	AVAILABLE	2026-09-08 05:59:24.686192	2026-10-05 05:59:24.686192	\N	\N	\N
cd625ac5-40bc-35b6-bd22-c73b1b727e32	22222222-2222-2222-2222-222222222222	66789058-6fa0-4ae1-8861-8ccf2055fbd4	Стан хороший.	\N	\N	\N	{PICKUP,MAIL}	AVAILABLE	2026-09-09 05:59:24.686192	2026-10-05 05:59:24.686192	\N	\N	\N
ed25e8c1-df17-252e-3c91-0fb53cee507f	33333333-3333-3333-3333-333333333333	ec10989f-c393-4909-9dc8-1694234cd286	Читана акуратно.	\N	\N	\N	{PICKUP}	AVAILABLE	2026-09-10 05:59:24.686192	2026-10-05 05:59:24.686192	\N	\N	\N
53035481-d05f-80c8-93de-e25c334b740e	44444444-4444-4444-4444-444444444444	13c74ae5-5062-4750-9a34-4f696be53a74	Є позначки.	\N	\N	\N	{PICKUP,MAIL}	AVAILABLE	2026-09-11 05:59:24.686192	2026-10-05 05:59:24.686192	\N	\N	\N
6222e633-cbd5-46a2-3e5e-642e444ab6ad	55555555-5555-5555-5555-555555555555	80d9cb61-ce54-4db9-97b4-d64e3703ea94	Майже нова.	\N	\N	\N	{MAIL}	AVAILABLE	2026-09-12 05:59:24.686192	2026-10-05 05:59:24.686192	\N	\N	\N
c500c659-929e-eb77-2ac2-ed31eada287a	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	21052470-8496-45cb-90da-dc6f262ef9c4	Гарний стан.	\N	\N	\N	{PICKUP}	AVAILABLE	2026-09-14 05:59:24.686192	2026-10-05 05:59:24.686192	\N	\N	\N
2ce3eb86-46b2-32dc-851c-cf9ee3435fed	4d909f2a-4d9a-43fb-9ba4-273c6270b2a0	97a743f8-ddd5-4138-8453-1d87f710b6db	Легкі потертості.	\N	\N	\N	{PICKUP,MAIL}	AVAILABLE	2026-09-16 05:59:24.686192	2026-10-05 05:59:24.686192	\N	\N	\N
32ca0ec0-b7f2-cd10-d8f6-e86de458cce1	11111111-1111-1111-1111-111111111111	762068c0-2ce8-4010-a206-5ef25193e699	Чудовий стан.	\N	\N	\N	{MAIL}	AVAILABLE	2026-09-18 05:59:24.686192	2026-10-05 05:59:24.686192	\N	\N	\N
697ee990-2e0c-630f-ddaa-2f14e3dfd7c6	22222222-2222-2222-2222-222222222222	273a23e1-42f1-499d-9a3f-4c6f60df6b00	Без пошкоджень.	\N	\N	\N	{PICKUP}	AVAILABLE	2026-09-19 05:59:24.686192	2026-10-05 05:59:24.686192	\N	\N	\N
355fce8c-6d4f-ceff-2004-a3d47e8790e9	33333333-3333-3333-3333-333333333333	9238eb19-f568-44c5-84ab-251660f95da8	Стан хороший.	\N	\N	\N	{PICKUP,MAIL}	AVAILABLE	2026-09-21 05:59:24.686192	2026-10-05 05:59:24.686192	\N	\N	\N
b0f5845f-1e2c-ee33-3ccc-e3d3bbdc2218	44444444-4444-4444-4444-444444444444	a564feff-6a38-47c2-9193-0ae3c3d2537d	Читана акуратно.	\N	\N	\N	{PICKUP}	AVAILABLE	2026-09-22 05:59:24.686192	2026-10-05 05:59:24.686192	\N	\N	\N
5262364a-0e33-55fb-d33a-654f6b13fe64	55555555-5555-5555-5555-555555555555	ec2fe2e3-9ec2-4dc8-81a9-c5b4f2dd5894	Є позначки.	\N	\N	\N	{MAIL}	AVAILABLE	2026-09-23 05:59:24.686192	2026-10-05 05:59:24.686192	\N	\N	\N
747089d8-260a-475a-974d-e94b4b0c8bd5	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	282352bf-e7c0-4a34-a9fa-a99565c848fa	Майже нова.	\N	\N	\N	{PICKUP,MAIL}	AVAILABLE	2026-09-24 05:59:24.686192	2026-10-05 05:59:24.686192	\N	\N	\N
97c7617d-7de5-3211-c346-7e1193ca596e	4d909f2a-4d9a-43fb-9ba4-273c6270b2a0	21001890-523d-4768-b6be-07dbc727630a	Гарний стан.	\N	\N	\N	{PICKUP}	AVAILABLE	2026-09-25 05:59:24.686192	2026-10-05 05:59:24.686192	\N	\N	\N
7954becf-2e69-f20a-fb6d-56c35639fbc7	11111111-1111-1111-1111-111111111111	196e8b9c-d397-40f9-b5ac-25411475b20c	Легкі потертості.	\N	\N	\N	{MAIL}	AVAILABLE	2026-09-26 05:59:24.686192	2026-10-05 05:59:24.686192	\N	\N	\N
f716eee7-5246-a987-9fe3-3d10f101b0df	22222222-2222-2222-2222-222222222222	4d1b7a1b-1c2b-4203-ad56-0c4660a2ffb0	Чудовий стан.	\N	\N	\N	{PICKUP,MAIL}	AVAILABLE	2026-09-27 05:59:24.686192	2026-10-05 05:59:24.686192	\N	\N	\N
09734a06-8694-01c5-e1ff-1858630851e8	33333333-3333-3333-3333-333333333333	1adc869a-0678-4f1e-93ee-8f0ecc0190d3	Без пошкоджень.	\N	\N	\N	{PICKUP}	AVAILABLE	2026-08-30 05:59:24.686192	2026-10-05 05:59:24.686192	\N	\N	\N
3e10d46f-d2a4-4be4-bac6-9d26cb97d53f	44444444-4444-4444-4444-444444444444	e201b2d8-0368-4753-a923-50612047e8cd	Стан хороший.	\N	\N	\N	{MAIL}	AVAILABLE	2026-08-31 05:59:24.686192	2026-10-05 05:59:24.686192	\N	\N	\N
4dd9dad8-d523-6cd0-9149-ef2cf6240e83	55555555-5555-5555-5555-555555555555	e9c1153c-2dca-4332-84be-02e6acb7c626	Читана акуратно.	\N	\N	\N	{PICKUP,MAIL}	AVAILABLE	2026-09-02 05:59:24.686192	2026-10-05 05:59:24.686192	\N	\N	\N
3de7b3db-9007-a36f-a9eb-96afc53b4e65	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	86caa903-2f60-45fa-bac2-db32c6e46311	Є позначки.	\N	\N	\N	{PICKUP}	AVAILABLE	2026-09-04 05:59:24.686192	2026-10-05 05:59:24.686192	\N	\N	\N
32e3c82f-071a-c506-f382-c74e443b9cf2	4d909f2a-4d9a-43fb-9ba4-273c6270b2a0	a1ae3063-07df-41c1-b895-42039175a64c	Майже нова.	\N	\N	\N	{MAIL}	AVAILABLE	2026-09-06 05:59:24.686192	2026-10-05 05:59:24.686192	\N	\N	\N
0954d883-2ff4-8a36-1af7-e68b508fb8a6	11111111-1111-1111-1111-111111111111	85866955-e1cb-41c4-b40b-c6efaae584a6	Гарний стан.	\N	\N	\N	{PICKUP,MAIL}	AVAILABLE	2026-09-15 05:59:24.686192	2026-10-05 05:59:24.686192	\N	\N	\N
765e9e28-be11-a736-4720-b2f2bc8dc33f	22222222-2222-2222-2222-222222222222	edca2a98-9944-40a8-9377-bbe21d46726c	Легкі потертості.	\N	\N	\N	{PICKUP}	AVAILABLE	2026-09-17 05:59:24.686192	2026-10-05 05:59:24.686192	\N	\N	\N
bf67b89d-9ed4-d73c-07d5-b37f8d6a9de6	33333333-3333-3333-3333-333333333333	58db7795-dc1b-4746-bce9-223e36be87e5	Чудовий стан.	\N	\N	\N	{MAIL}	AVAILABLE	2026-09-20 05:59:24.686192	2026-10-05 05:59:24.686192	\N	\N	\N
6c883e87-4c86-2861-e5fd-0064aefdec18	44444444-4444-4444-4444-444444444444	ff6887fd-cd06-4858-886d-2ca4a17df394	Без пошкоджень.	\N	\N	\N	{PICKUP,MAIL}	AVAILABLE	2026-09-28 05:59:24.686192	2026-10-05 05:59:24.686192	\N	\N	\N
f29268e9-7f58-36d4-bb45-874297be6cf5	55555555-5555-5555-5555-555555555555	fd95c0a9-b640-427a-9d4d-0ab35734429e	Стан хороший.	\N	\N	\N	{PICKUP}	AVAILABLE	2026-09-29 05:59:24.686192	2026-10-05 05:59:24.686192	\N	\N	\N
7a16db7f-8372-bdda-d3a5-93fbab2ec3b3	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	2ecadb18-2770-4714-96c5-63d7e7484a0c	Читана акуратно.	\N	\N	\N	{MAIL}	AVAILABLE	2026-09-30 05:59:24.686192	2026-10-05 05:59:24.686192	\N	\N	\N
a2da06de-1b56-c01b-d1fa-d31f32e27094	4d909f2a-4d9a-43fb-9ba4-273c6270b2a0	203ba202-c3db-42f8-9fe5-23209f7fc579	Є позначки.	\N	\N	\N	{PICKUP,MAIL}	AVAILABLE	2026-10-01 05:59:24.686192	2026-10-05 05:59:24.686192	\N	\N	\N
4cbaf7fc-023c-28c7-3d83-75526b9d5eac	11111111-1111-1111-1111-111111111111	91c56c21-ac9d-4fca-aa59-af25f2e8e545	Майже нова.	\N	\N	\N	{PICKUP}	AVAILABLE	2026-10-02 05:59:24.686192	2026-10-05 05:59:24.686192	\N	\N	\N
5e4a5eda-cf31-a444-7964-852c603a9ca1	22222222-2222-2222-2222-222222222222	aa8cadcf-4612-4d52-ab55-7da5fba850d8	Гарний стан.	\N	\N	\N	{MAIL}	AVAILABLE	2026-10-03 05:59:24.686192	2026-10-05 05:59:24.686192	\N	\N	\N
b0cc2882-be85-f28b-4188-ffc6fc00344a	33333333-3333-3333-3333-333333333333	f804e166-5298-4e68-b517-f674fffe74f1	Легкі потертості.	\N	\N	\N	{PICKUP,MAIL}	AVAILABLE	2026-08-24 05:59:24.686192	2026-10-05 05:59:24.686192	\N	\N	\N
174c3c7e-7316-5cb1-4d19-e3b6fa3168f8	44444444-4444-4444-4444-444444444444	ea28ebbb-f5df-4d45-862b-3646f8f6c4f9	Чудовий стан.	\N	\N	\N	{PICKUP}	AVAILABLE	2026-08-22 05:59:24.686192	2026-10-05 05:59:24.686192	\N	\N	\N
f1a10000-0000-0000-0000-000000000001	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	1b670d23-6146-4517-871a-e755d24c8bc8	Стан хороший, кілька заломів на обкладинці.	\N	\N	\N	{PICKUP,MAIL}	RESERVED	2026-09-30 05:59:24.686192	2026-10-16 08:56:03.593388	\N	\N	\N
4c215420-d0c0-4781-9ce5-2c0ec598b64e	4d909f2a-4d9a-43fb-9ba4-273c6270b2a0	61230c0f-8e42-4489-bd7a-84c3909c1a44	Ідеальний стан книги	CITY	Одеська область	Одеса	{PICKUP,MAIL}	AVAILABLE	2026-10-19 17:36:10.151563	2026-10-19 17:49:21.816971	\N	\N	\N
\.


--
-- Data for Name: moderation_actions; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.moderation_actions (id, moderator_id, action, target_type, target_id, target_user_id, reason, details, created_at) FROM stdin;
\.


--
-- Data for Name: notifications; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.notifications (id, user_id, type, reference_id, message, is_read, created_at) FROM stdin;
11ac6bfa-efc5-43ba-ab0a-b5a3baf37265	11111111-1111-1111-1111-111111111111	NEW_REQUEST	c3333333-0000-0000-0000-000000000001	Нова заявка на вашу книгу "Затьмарення".	t	2026-09-10 05:43:14.643994
8f9c782f-256d-49bf-8384-c3a8514179cf	33333333-3333-3333-3333-333333333333	NEW_REQUEST	c3333333-0000-0000-0000-000000000003	Нова заявка на вашу книгу "Хроніки Амбера".	f	2026-10-02 05:43:14.643994
586ae04f-c9df-46cd-8b02-4635f6694994	55555555-5555-5555-5555-555555555555	DISPUTE_RESOLVED	e5555555-0000-0000-0000-000000000001	Скаргу щодо вас розглянуто модератором. Акаунт тимчасово обмежено.	f	2026-09-02 05:43:14.643994
d6283b77-9a0e-463e-8831-8cd9dc1d210d	44444444-4444-4444-4444-444444444444	REVIEW_RECEIVED	d4444444-0000-0000-0000-000000000003	Ви отримали новий відгук від Марії.	f	2026-09-23 05:43:14.643994
f6a60000-0000-0000-0000-000000000002	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	REQUEST_REJECTED	f2a20000-0000-0000-0000-000000000002	Вашу заявку відхилено: На жаль, дедлайн занадто короткий для мене	t	2026-09-30 05:59:24.686192
a71db700-b670-4eab-8792-56b1aac72f8e	44444444-4444-4444-4444-444444444444	NEW_REQUEST	39bee8a4-4c1d-4a0a-ad18-8023f335b7b2	Нова заявка на вашу книгу "Кінець дитинства"	f	2026-10-06 14:47:20.360617
9cb19a0f-2ec3-436e-b130-352199dad99a	4d909f2a-4d9a-43fb-9ba4-273c6270b2a0	REQUEST_APPROVED	f2a20000-0000-0000-0000-000000000001	Вашу заявку на книгу "Дюна" підтверджено	f	2026-10-16 08:56:03.589334
dd7fe667-afd6-4feb-ac26-024f5e9a7a1e	44444444-4444-4444-4444-444444444444	NEW_REQUEST	a5e444f2-aeb7-4a81-91f0-c0ce85ed266f	Нова заявка на вашу книгу "Кінець дитинства"	f	2026-10-16 09:32:55.002857
864eada2-f0d1-4676-80ca-05e684d910b5	22222222-2222-2222-2222-222222222222	REVIEW_RECEIVED	f3a30000-0000-0000-0000-000000000008	Ви отримали новий відгук від Dmytro Babych.	f	2026-10-16 16:15:08.358718
45021386-83b3-4882-abf9-7c6cc254771c	22222222-2222-2222-2222-222222222222	NEW_REQUEST	f12f0ece-fc64-4ff7-a624-c7e744c2a453	Нова заявка на вашу книгу "Робінзон Крузо"	f	2026-10-19 17:33:14.205364
b793805d-13e5-4cd6-9d58-fedb6f87ce27	4d909f2a-4d9a-43fb-9ba4-273c6270b2a0	NEW_REQUEST	e8c068b5-25d5-43e4-8f6d-6eee0531bd63	Нова заявка на вашу книгу "Атомні звички. Легкий і перевірений спосіб набути корисних звичок і позбутися звичок шкідливих"	f	2026-10-19 17:37:02.578484
d3edc676-73da-4ff3-a408-278764031539	4d909f2a-4d9a-43fb-9ba4-273c6270b2a0	EXTENSION_REQUESTED	1dac1b3d-76c3-4610-9edb-6ff626528da3	Читач просить продовжити читання на 7 дн. для "Атомні звички. Легкий і перевірений спосіб набути корисних звичок і позбутися звичок шкідливих"	f	2026-10-19 17:44:23.798196
a237d150-6e45-4971-948d-c3812baedef3	4d909f2a-4d9a-43fb-9ba4-273c6270b2a0	EXTENSION_DECIDED	f3a30000-0000-0000-0000-000000000005	У продовженні дедлайну відмовлено	f	2026-10-19 17:50:29.013852
5e4be300-a39d-4e5f-9dc3-629fd69f513f	44444444-4444-4444-4444-444444444444	NEW_COMMENT	11223344-0000-0000-0000-000000000002	Dmytro Babych прокоментував(-ла) ваш пост	f	2026-10-19 17:50:58.720876
f585f6f8-7843-460f-84e6-73bd1bee7015	22222222-2222-2222-2222-222222222222	DEADLINE_OVERDUE	d4444444-0000-0000-0000-000000000001	Термін читання «Затьмарення» минув 19.10.2026. Поверніть книгу якнайшвидше	f	2026-10-26 12:05:00.097472
7ab8bbcb-0bee-474c-a975-61584a1f6800	11111111-1111-1111-1111-111111111111	DEADLINE_OVERDUE	d4444444-0000-0000-0000-000000000001	Марія Шевченко не повернув(-ла) «Затьмарення» вчасно. Напишіть у чат	f	2026-10-26 12:05:00.129532
b81311a5-3eef-4a5d-bf01-2a4b6763cbb1	4d909f2a-4d9a-43fb-9ba4-273c6270b2a0	DEADLINE_OVERDUE	f3a30000-0000-0000-0000-000000000002	Dmytro Babych не повернув(-ла) «Гра престолів» вчасно. Напишіть у чат	f	2026-10-26 12:05:00.289976
f11dd450-12c6-4d3c-995f-2764ef6e73d8	4d909f2a-4d9a-43fb-9ba4-273c6270b2a0	DEADLINE_OVERDUE	f3a30000-0000-0000-0000-000000000005	Термін читання «Воно» минув 27.09.2026. Поверніть книгу якнайшвидше	f	2026-10-26 12:05:00.316027
152f4bd2-1124-4476-964c-ccf6461a53d3	11111111-1111-1111-1111-111111111111	DEADLINE_OVERDUE	f3a30000-0000-0000-0000-000000000007	Термін читання «Знедолені» минув 11.10.2026. Поверніть книгу якнайшвидше	f	2026-10-26 12:05:00.333484
f6a60000-0000-0000-0000-000000000001	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	NEW_REQUEST	f2a20000-0000-0000-0000-000000000001	Нова заявка на вашу книгу	t	2026-10-03 05:59:24.686192
f6a60000-0000-0000-0000-000000000003	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	REVIEW_RECEIVED	f3a30000-0000-0000-0000-000000000004	Ви отримали новий відгук від Дмитра	t	2026-10-01 05:59:24.686192
b0399cb4-6c9e-4a3d-8db0-09831676bfc3	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	REQUEST_APPROVED	e8c068b5-25d5-43e4-8f6d-6eee0531bd63	Вашу заявку на книгу "Атомні звички. Легкий і перевірений спосіб набути корисних звичок і позбутися звичок шкідливих" підтверджено	t	2026-10-19 17:38:13.066941
cbb4ca9b-3449-4dd3-8ff4-7fb71013dc75	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	EXTENSION_DECIDED	1dac1b3d-76c3-4610-9edb-6ff626528da3	Продовження дедлайну підтверджено	t	2026-10-19 17:47:55.627555
d4e1dd95-67a7-4771-b127-8835df53b611	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	REVIEW_RECEIVED	1dac1b3d-76c3-4610-9edb-6ff626528da3	Ви отримали новий відгук від Dmytro Babych.	t	2026-10-19 17:49:37.123357
0e512fb2-0dec-47be-9194-174311f0a656	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	EXTENSION_REQUESTED	f3a30000-0000-0000-0000-000000000005	Читач просить продовжити читання на 30 дн. для "Воно"	t	2026-10-19 17:49:50.754588
36f3e087-8762-44f3-a40f-fc6c5577f642	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	DEADLINE_OVERDUE	f3a30000-0000-0000-0000-000000000002	Термін читання «Гра престолів» минув 13.10.2026. Поверніть книгу якнайшвидше	t	2026-10-26 12:05:00.163895
c0cc7437-4eb4-4ffd-8ef5-e20923bd687f	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	DEADLINE_OVERDUE	f3a30000-0000-0000-0000-000000000005	Dmytro Babych не повернув(-ла) «Воно» вчасно. Напишіть у чат	t	2026-10-26 12:05:00.321664
cd39d7ed-430d-47b5-b4f3-f2b3548c730d	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	DEADLINE_OVERDUE	f3a30000-0000-0000-0000-000000000007	Олег Коваленко не повернув(-ла) «Знедолені» вчасно. Напишіть у чат	t	2026-10-26 12:05:00.337962
05792d53-157c-41ea-8aa5-6b3c3e777c38	44444444-4444-4444-4444-444444444444	NEW_REQUEST	b3916b22-5ee7-46b4-939b-eec62966d749	Нова заявка на вашу книгу "Великий Гетсбі"	f	2026-10-26 12:35:03.743214
8fa76ac5-f470-439e-b578-dca2e96a78ce	4d909f2a-4d9a-43fb-9ba4-273c6270b2a0	NEW_REQUEST	40e1587d-fb92-4f2c-bfa7-b4d92df86741	Нова заявка на вашу книгу "Атомні звички. Легкий і перевірений спосіб набути корисних звичок і позбутися звичок шкідливих"	t	2026-10-26 12:35:41.225689
82b9c389-08f7-4fa2-b302-560dacb1812a	4d909f2a-4d9a-43fb-9ba4-273c6270b2a0	EXTENSION_REQUESTED	f3a30000-0000-0000-0000-000000000002	Dmytro Babych просить продовжити читання «Гра престолів» на 30 дн.	t	2026-10-27 10:58:36.68792
b4139366-c16e-41f8-8ba5-8ff021db64c4	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	EXTENSION_DECIDED	f3a30000-0000-0000-0000-000000000002	Термін читання «Гра престолів» продовжено до 12.11.2026	f	2026-10-27 15:43:22.839537
6667a63b-fef0-4639-9741-f8a776a3eb9b	55555555-5555-5555-5555-555555555555	ACCOUNT_RESTORED	\N	Строк обмеження минув — ви знову можете додавати книги й подавати заявки	f	2026-10-27 16:00:00.03486
\.


--
-- Data for Name: post_likes; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.post_likes (id, post_id, user_id, created_at) FROM stdin;
bdc98dfd-f4bb-4d0d-bddb-5349743fccec	11223344-0000-0000-0000-000000000001	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	2026-10-19 15:01:56.779715
ef97a1b0-015f-4fe8-bda4-1c2c45e9ee5f	11223344-0000-0000-0000-000000000002	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	2026-10-26 12:09:04.063859
\.


--
-- Data for Name: posts; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.posts (id, author_id, content, photo_url, created_at) FROM stdin;
11223344-0000-0000-0000-000000000001	11111111-1111-1111-1111-111111111111	Щойно дочитав "Затьмарення" Філіпа Діка — важка, але дуже сильна книга. Рекомендую!	\N	2026-09-20 05:43:14.592881
11223344-0000-0000-0000-000000000002	44444444-4444-4444-4444-444444444444	Зібрала невелику поличку книг для обміну цього місяця, дивіться мої оголошення.	https://photos.example.com/post2.jpg	2026-09-27 05:43:14.592881
f2a9f503-872e-4244-9c18-5021e3e9182a	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	Порекомендуйте що почитати	\N	2026-10-19 15:05:43.597444
\.


--
-- Data for Name: requests; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.requests (id, listing_id, requester_id, preferred_delivery_method, message, status, reject_comment, created_at, decided_at, reading_days) FROM stdin;
c3333333-0000-0000-0000-000000000001	b2222222-0000-0000-0000-000000000001	22222222-2222-2222-2222-222222222222	MAIL	Дуже хочу прочитати, поверну вчасно!	ACTIVE	\N	2026-09-10 05:43:14.180616	2026-09-11 05:43:14.180616	39
c3333333-0000-0000-0000-000000000002	b2222222-0000-0000-0000-000000000004	22222222-2222-2222-2222-222222222222	MAIL	\N	ACTIVE	\N	2026-09-30 05:43:14.180616	2026-10-01 05:43:14.180616	15
c3333333-0000-0000-0000-000000000003	b2222222-0000-0000-0000-000000000004	44444444-4444-4444-4444-444444444444	MAIL	Можна довше почитати?	PENDING	\N	2026-10-02 05:43:14.180616	\N	23
c3333333-0000-0000-0000-000000000004	b2222222-0000-0000-0000-000000000004	55555555-5555-5555-5555-555555555555	MAIL	\N	REJECTED	На жаль, дедлайн занадто короткий і є прострочення в історії.	2026-10-01 05:43:14.180616	2026-10-01 05:43:14.180616	11
c3333333-0000-0000-0000-000000000005	b2222222-0000-0000-0000-000000000002	44444444-4444-4444-4444-444444444444	PICKUP	\N	ACTIVE	\N	2026-08-06 05:43:14.359762	2026-08-07 05:43:14.359762	50
c3333333-0000-0000-0000-000000000006	b2222222-0000-0000-0000-000000000003	55555555-5555-5555-5555-555555555555	PICKUP	\N	ACTIVE	\N	2026-07-27 05:43:14.44484	2026-07-28 05:43:14.44484	30
f2a20000-0000-0000-0000-000000000002	f1a10000-0000-0000-0000-000000000002	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	PICKUP	\N	REJECTED	На жаль, дедлайн занадто короткий для мене	2026-09-29 05:59:24.686192	2026-09-30 05:59:24.686192	9
f2a20000-0000-0000-0000-000000000003	f1a10000-0000-0000-0000-000000000003	4d909f2a-4d9a-43fb-9ba4-273c6270b2a0	MAIL	\N	CANCELLED	\N	2026-09-30 05:59:24.686192	2026-10-01 05:59:24.686192	15
f2a20000-0000-0000-0000-000000000004	f1a10000-0000-0000-0000-000000000004	4d909f2a-4d9a-43fb-9ba4-273c6270b2a0	PICKUP	Поверну точно вчасно.	ACTIVE	\N	2026-10-01 05:59:24.686192	2026-10-02 05:59:24.686192	16
f2a20000-0000-0000-0000-000000000005	f1a10000-0000-0000-0000-000000000005	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	MAIL	\N	ACTIVE	\N	2026-09-25 05:59:24.686192	2026-09-26 05:59:24.686192	18
f2a20000-0000-0000-0000-000000000006	f1a10000-0000-0000-0000-000000000006	4d909f2a-4d9a-43fb-9ba4-273c6270b2a0	MAIL	\N	ACTIVE	\N	2026-09-20 05:59:24.686192	2026-09-21 05:59:24.686192	17
f2a20000-0000-0000-0000-000000000007	f1a10000-0000-0000-0000-000000000007	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	PICKUP	\N	ACTIVE	\N	2026-09-15 05:59:24.686192	2026-09-16 05:59:24.686192	15
f2a20000-0000-0000-0000-000000000008	f1a10000-0000-0000-0000-000000000008	4d909f2a-4d9a-43fb-9ba4-273c6270b2a0	PICKUP	\N	ACTIVE	\N	2026-09-10 05:59:24.686192	2026-09-11 05:59:24.686192	17
f2a20000-0000-0000-0000-000000000009	f1a10000-0000-0000-0000-000000000009	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	MAIL	\N	ACTIVE	\N	2026-09-17 05:59:24.686192	2026-09-18 05:59:24.686192	16
f2a20000-0000-0000-0000-000000000010	f1a10000-0000-0000-0000-000000000010	11111111-1111-1111-1111-111111111111	PICKUP	\N	ACTIVE	\N	2026-09-26 05:59:24.686192	2026-09-27 05:59:24.686192	15
f2a20000-0000-0000-0000-000000000011	f1a10000-0000-0000-0000-000000000011	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	PICKUP	\N	ACTIVE	\N	2026-09-13 05:59:24.686192	2026-09-14 05:59:24.686192	12
39bee8a4-4c1d-4a0a-ad18-8023f335b7b2	d2aca80f-1fd0-4089-b019-de7be1a4b4b4	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	MAIL	Бла бла бла	CANCELLED	\N	2026-10-06 14:47:20.354708	2026-10-16 08:55:14.941659	43
f2a20000-0000-0000-0000-000000000001	f1a10000-0000-0000-0000-000000000001	4d909f2a-4d9a-43fb-9ba4-273c6270b2a0	PICKUP	Давно хотів цю книгу прочитати!	ACTIVE	\N	2026-10-03 05:59:24.686192	2026-10-16 08:56:03.571161	16
a5e444f2-aeb7-4a81-91f0-c0ce85ed266f	d2aca80f-1fd0-4089-b019-de7be1a4b4b4	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	PICKUP	Привіт, цікавить книга, давно хотів прочитати	CANCELLED	\N	2026-10-16 09:32:55.00013	2026-10-16 09:33:07.545809	12
e8c068b5-25d5-43e4-8f6d-6eee0531bd63	4c215420-d0c0-4781-9ce5-2c0ec598b64e	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	MAIL		ACTIVE	\N	2026-10-19 17:37:02.57743	2026-10-19 17:38:13.055308	80
f12f0ece-fc64-4ff7-a624-c7e744c2a453	5e4a5eda-cf31-a444-7964-852c603a9ca1	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	MAIL		CANCELLED	\N	2026-10-19 17:33:14.192356	2026-10-20 13:53:57.826686	80
b3916b22-5ee7-46b4-939b-eec62966d749	490cd468-99f2-49cf-a63f-eed79e41da04	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	PICKUP		PENDING	\N	2026-10-26 12:35:03.728544	\N	80
40e1587d-fb92-4f2c-bfa7-b4d92df86741	4c215420-d0c0-4781-9ce5-2c0ec598b64e	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	MAIL		PENDING	\N	2026-10-26 12:35:41.224611	\N	30
\.


--
-- Data for Name: restrictions; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.restrictions (id, user_id, type, reason, dispute_id, starts_at, ends_at, created_by, lifted_at, lifted_by) FROM stdin;
e02b14ab-8aab-41ea-bb94-4cd612458384	55555555-5555-5555-5555-555555555555	TEMPORARY	Прострочення повернення книги понад 30 днів та підтверджене пошкодження.	e5555555-0000-0000-0000-000000000001	2026-09-02 05:43:14.527643	2026-10-19 05:43:14.527643	\N	\N	\N
\.


--
-- Data for Name: reviews; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.reviews (id, exchange_id, author_id, target_id, rating, comment, created_at) FROM stdin;
0a43ad7f-b4ff-4bcf-bb88-325923866201	d4444444-0000-0000-0000-000000000003	22222222-2222-2222-2222-222222222222	44444444-4444-4444-4444-444444444444	5	Чудовий читач, повернула книгу в ідеальному стані і навіть раніше терміном!	2026-09-23 05:43:14.415714
69cccc18-5d08-4712-9fee-50353e656f2a	d4444444-0000-0000-0000-000000000003	44444444-4444-4444-4444-444444444444	22222222-2222-2222-2222-222222222222	5	Дуже приємне спілкування, книга була саме такою, як описано.	2026-09-23 05:43:14.415714
f5a50000-0000-0000-0000-000000000001	f3a30000-0000-0000-0000-000000000004	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	4d909f2a-4d9a-43fb-9ba4-273c6270b2a0	5	Чудовий власник, книга була саме такою, як описано!	2026-10-01 05:59:24.686192
f5a50000-0000-0000-0000-000000000002	f3a30000-0000-0000-0000-000000000004	4d909f2a-4d9a-43fb-9ba4-273c6270b2a0	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	5	Повернув вчасно, у ідеальному стані. Рекомендую!	2026-10-01 05:59:24.686192
f5a50000-0000-0000-0000-000000000003	f3a30000-0000-0000-0000-000000000008	22222222-2222-2222-2222-222222222222	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	5	Дуже приємний читач, швидко повернув книгу.	2026-09-29 05:59:24.686192
224a748f-2746-4f0c-891d-3050babb9222	f3a30000-0000-0000-0000-000000000008	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	22222222-2222-2222-2222-222222222222	5	Власник СУПЕР!	2026-10-16 16:15:08.333349
e1e18b6b-751e-47f2-96a6-732f47e543b3	1dac1b3d-76c3-4610-9edb-6ff626528da3	4d909f2a-4d9a-43fb-9ba4-273c6270b2a0	8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	5	Все вчасно, рекомендую до спільних обмінів!	2026-10-19 17:49:37.109741
\.


--
-- Data for Name: shipment_info; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.shipment_info (id, exchange_id, direction, recipient_name, recipient_phone, carrier, city, branch_number, waybill_photo_url, status, shipped_at, delivered_at) FROM stdin;
a238e56c-2586-4109-8a69-00dad029e47d	d4444444-0000-0000-0000-000000000001	TO_READER	Марія Шевченко	+380671234567	NOVA_POSHTA	Львів	24	https://photos.example.com/waybill1.jpg	DELIVERED	2026-09-12 05:43:14.331439	2026-09-14 05:43:14.331439
a1b10000-0000-0000-0000-000000000001	f3a30000-0000-0000-0000-000000000002	TO_READER	Dmytro Babych	+380671112233	NOVA_POSHTA	Київ	24	https://picsum.photos/seed/waybill-exch2/400/300	DELIVERED	2026-09-26 05:59:24.686192	2026-09-28 05:59:24.686192
a1b10000-0000-0000-0000-000000000002	f3a30000-0000-0000-0000-000000000003	TO_READER	Dmytro Babych	+380671112233	NOVA_POSHTA	Одеса	15	https://picsum.photos/seed/waybill-exch3a/400/300	DELIVERED	2026-09-22 05:59:24.686192	2026-09-24 05:59:24.686192
a1b10000-0000-0000-0000-000000000003	f3a30000-0000-0000-0000-000000000003	TO_OWNER	Dmytro Babych	+380671112233	UKRPOSHTA	Київ	7	\N	PENDING	\N	\N
a1b10000-0000-0000-0000-000000000004	f3a30000-0000-0000-0000-000000000006	TO_READER	Dmytro Babych	+380671112233	NOVA_POSHTA	Київ	24	https://picsum.photos/seed/waybill-exch6a/400/300	DELIVERED	2026-09-19 05:59:24.686192	2026-09-21 05:59:24.686192
a1b10000-0000-0000-0000-000000000005	f3a30000-0000-0000-0000-000000000006	TO_OWNER	Dmytro Babych	+380671112233	NOVA_POSHTA	Київ	24	https://picsum.photos/seed/waybill-exch6b/400/300	SHIPPED	2026-10-04 05:59:24.686192	\N
67556051-3253-4557-8d3f-6a8ea102eb8e	1dac1b3d-76c3-4610-9edb-6ff626528da3	TO_READER	Бабич Дмитро	+380683150700	NOVA_POSHTA	Київ	320	http://localhost:8080/uploads/fa26e66e-b1a3-4382-b3b2-d3ef8e777c95.png	DELIVERED	2026-10-19 17:42:01.513122	2026-10-19 17:42:19.727539
81ea7005-2942-4373-8634-058492479c3b	1dac1b3d-76c3-4610-9edb-6ff626528da3	TO_OWNER	Дмитро Бабич	+380683150700	NOVA_POSHTA	Одеса	210	http://localhost:8080/uploads/6dfa759d-2585-4ae7-a345-eb88bae779c8.png	DELIVERED	2026-10-19 17:49:05.454166	2026-10-19 17:49:21.804106
\.


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.users (id, google_id, email, password_hash, name, avatar_url, settlement_type, region, settlement_name, bio, rating_avg, books_taken, books_returned_on_time, books_overdue, books_damaged, books_given, role, status, restricted_until, created_at, updated_at) FROM stdin;
11111111-1111-1111-1111-111111111111	g-oleh	oleh.k@example.com	\N	Олег Коваленко	https://i.pravatar.cc/150?u=oleh	CITY	Київська область	Київ	Люблю фантастику і детективи.	4.80	5	5	0	0	8	USER	ACTIVE	\N	2026-04-08 05:43:13.902666	2026-10-05 05:43:13.902666
33333333-3333-3333-3333-333333333333	g-ivan	ivan.p@example.com	\N	Іван Петренко	https://i.pravatar.cc/150?u=ivan	CITY	Одеська область	Одеса	Нон-фікшн та саморозвиток.	3.90	6	4	2	1	2	USER	ACTIVE	\N	2026-06-07 05:43:13.902666	2026-10-05 05:43:13.902666
44444444-4444-4444-4444-444444444444	g-anna	anna.d@example.com	\N	Анна Дмитренко	https://i.pravatar.cc/150?u=anna	CITY	Харківська область	Харків	Читаю все підряд.	5.00	2	2	0	0	6	USER	ACTIVE	\N	2026-07-07 05:43:13.902666	2026-10-05 05:43:13.902666
66666666-6666-6666-6666-666666666666	\N	moderator@bookshare.ua	\N	Модератор Сервісу	https://i.pravatar.cc/150?u=moderator	CITY	Київська область	Київ	\N	0.00	0	0	0	0	0	MODERATOR	ACTIVE	\N	2025-10-05 05:43:13.902666	2026-10-05 05:43:13.902666
22222222-2222-2222-2222-222222222222	g-maria	maria.s@example.com	\N	Марія Шевченко	https://i.pravatar.cc/150?u=maria	CITY	Львівська область	Львів	Класика і поезія.	5.00	3	2	1	0	4	USER	ACTIVE	\N	2026-05-08 05:43:13.902666	2026-10-16 16:15:08.361426
8a64b9d2-c311-42d4-85a1-2dbaaf7c64b9	117198698519833406462	dimikoff008@gmail.com	\N	Dmytro Babych	https://lh3.googleusercontent.com/a/ACg8ocKhQXXvWaQoG7ISsp2kkRhUWhJl14GnhQc8bPT2NdU8zvqbY_w=s96-c	CITY	Київська область	Київ		5.00	1	1	0	0	0	USER	ACTIVE	\N	2026-10-05 08:43:31.003206	2026-10-19 17:49:37.124955
55555555-5555-5555-5555-555555555555	g-taras	taras.m@example.com	\N	Тарас Мельник	https://i.pravatar.cc/150?u=taras	CITY	Дніпропетровська область	Дніпро	Іноді забуваю повертати книги)	3.20	4	1	3	1	1	USER	ACTIVE	\N	2026-03-19 05:43:13.902666	2026-10-27 16:00:00.04653
4d909f2a-4d9a-43fb-9ba4-273c6270b2a0	101682846081195651180	babych.dmtr@gmail.com	\N	Dmytro Babych	https://lh3.googleusercontent.com/a/ACg8ocKmOm2YZP_8Kg-EmIXpl-ZSlObs0ezjMvr1HrhKGWIO_ZUB_yU=s96-c	CITY	Одеська область	Одеса	\N	5.00	0	0	0	0	1	MODERATOR	ACTIVE	\N	2026-10-05 08:44:40.93695	2026-10-19 17:49:21.821887
\.


--
-- Data for Name: wishlist_items; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.wishlist_items (id, user_id, book_catalog_entry_id, query_title, created_at) FROM stdin;
9b1f0393-2cd4-4058-912a-c1bff123eccc	33333333-3333-3333-3333-333333333333	a1111111-0000-0000-0000-000000000001	\N	2026-10-05 05:43:14.155701
3e4e643e-efb9-4aa4-b7d0-4873d61e4fda	55555555-5555-5555-5555-555555555555	\N	Дюна	2026-10-05 05:43:14.155701
\.


--
-- Name: book_catalog_entries book_catalog_entries_isbn_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.book_catalog_entries
    ADD CONSTRAINT book_catalog_entries_isbn_key UNIQUE (isbn);


--
-- Name: book_catalog_entries book_catalog_entries_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.book_catalog_entries
    ADD CONSTRAINT book_catalog_entries_pkey PRIMARY KEY (id);


--
-- Name: chat_messages chat_messages_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.chat_messages
    ADD CONSTRAINT chat_messages_pkey PRIMARY KEY (id);


--
-- Name: chat_rooms chat_rooms_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.chat_rooms
    ADD CONSTRAINT chat_rooms_pkey PRIMARY KEY (id);


--
-- Name: comments comments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.comments
    ADD CONSTRAINT comments_pkey PRIMARY KEY (id);


--
-- Name: deadline_extension_requests deadline_extension_requests_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.deadline_extension_requests
    ADD CONSTRAINT deadline_extension_requests_pkey PRIMARY KEY (id);


--
-- Name: deadline_reminders deadline_reminders_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.deadline_reminders
    ADD CONSTRAINT deadline_reminders_pkey PRIMARY KEY (exchange_id, kind, due_date);


--
-- Name: disputes disputes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.disputes
    ADD CONSTRAINT disputes_pkey PRIMARY KEY (id);


--
-- Name: exchange_photos exchange_photos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.exchange_photos
    ADD CONSTRAINT exchange_photos_pkey PRIMARY KEY (id);


--
-- Name: exchanges exchanges_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.exchanges
    ADD CONSTRAINT exchanges_pkey PRIMARY KEY (id);


--
-- Name: exchanges exchanges_request_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.exchanges
    ADD CONSTRAINT exchanges_request_id_key UNIQUE (request_id);


--
-- Name: feedback feedback_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.feedback
    ADD CONSTRAINT feedback_pkey PRIMARY KEY (id);


--
-- Name: listing_photos listing_photos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.listing_photos
    ADD CONSTRAINT listing_photos_pkey PRIMARY KEY (id);


--
-- Name: listings listings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.listings
    ADD CONSTRAINT listings_pkey PRIMARY KEY (id);


--
-- Name: moderation_actions moderation_actions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.moderation_actions
    ADD CONSTRAINT moderation_actions_pkey PRIMARY KEY (id);


--
-- Name: notifications notifications_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_pkey PRIMARY KEY (id);


--
-- Name: post_likes post_likes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.post_likes
    ADD CONSTRAINT post_likes_pkey PRIMARY KEY (id);


--
-- Name: posts posts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.posts
    ADD CONSTRAINT posts_pkey PRIMARY KEY (id);


--
-- Name: requests requests_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.requests
    ADD CONSTRAINT requests_pkey PRIMARY KEY (id);


--
-- Name: restrictions restrictions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.restrictions
    ADD CONSTRAINT restrictions_pkey PRIMARY KEY (id);


--
-- Name: reviews reviews_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reviews
    ADD CONSTRAINT reviews_pkey PRIMARY KEY (id);


--
-- Name: shipment_info shipment_info_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.shipment_info
    ADD CONSTRAINT shipment_info_pkey PRIMARY KEY (id);


--
-- Name: post_likes uq_post_likes_post_user; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.post_likes
    ADD CONSTRAINT uq_post_likes_post_user UNIQUE (post_id, user_id);


--
-- Name: users users_email_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE (email);


--
-- Name: users users_google_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_google_id_key UNIQUE (google_id);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: wishlist_items wishlist_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.wishlist_items
    ADD CONSTRAINT wishlist_items_pkey PRIMARY KEY (id);


--
-- Name: idx_chat_messages_room; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_chat_messages_room ON public.chat_messages USING btree (chat_room_id);


--
-- Name: idx_chat_messages_room_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_chat_messages_room_created ON public.chat_messages USING btree (chat_room_id, created_at DESC);


--
-- Name: idx_chat_messages_unread; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_chat_messages_unread ON public.chat_messages USING btree (chat_room_id, sender_id) WHERE (read_at IS NULL);


--
-- Name: idx_chat_rooms_user_a; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_chat_rooms_user_a ON public.chat_rooms USING btree (user_a_id);


--
-- Name: idx_chat_rooms_user_b; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_chat_rooms_user_b ON public.chat_rooms USING btree (user_b_id);


--
-- Name: idx_comments_post_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_comments_post_id ON public.comments USING btree (post_id);


--
-- Name: idx_exchanges_owner; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_exchanges_owner ON public.exchanges USING btree (owner_id);


--
-- Name: idx_exchanges_reader; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_exchanges_reader ON public.exchanges USING btree (reader_id);


--
-- Name: idx_exchanges_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_exchanges_status ON public.exchanges USING btree (status);


--
-- Name: idx_feedback_email_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_feedback_email_created ON public.feedback USING btree (lower((email)::text), created_at);


--
-- Name: idx_feedback_ip_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_feedback_ip_created ON public.feedback USING btree (ip_address, created_at);


--
-- Name: idx_feedback_status_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_feedback_status_created ON public.feedback USING btree (status, created_at DESC);


--
-- Name: idx_listings_hidden; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_listings_hidden ON public.listings USING btree (hidden_at) WHERE (hidden_at IS NOT NULL);


--
-- Name: idx_listings_owner; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_listings_owner ON public.listings USING btree (owner_id);


--
-- Name: idx_listings_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_listings_status ON public.listings USING btree (status);


--
-- Name: idx_moderation_actions_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_moderation_actions_created ON public.moderation_actions USING btree (created_at DESC);


--
-- Name: idx_moderation_actions_target_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_moderation_actions_target_user ON public.moderation_actions USING btree (target_user_id, created_at DESC);


--
-- Name: idx_notifications_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_notifications_user ON public.notifications USING btree (user_id, is_read);


--
-- Name: idx_notifications_user_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_notifications_user_created ON public.notifications USING btree (user_id, created_at DESC);


--
-- Name: idx_notifications_user_unread; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_notifications_user_unread ON public.notifications USING btree (user_id) WHERE (is_read = false);


--
-- Name: idx_post_likes_post_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_post_likes_post_id ON public.post_likes USING btree (post_id);


--
-- Name: idx_posts_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_posts_created_at ON public.posts USING btree (created_at DESC);


--
-- Name: idx_requests_listing; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_requests_listing ON public.requests USING btree (listing_id);


--
-- Name: idx_requests_requester; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_requests_requester ON public.requests USING btree (requester_id);


--
-- Name: idx_reviews_target; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_reviews_target ON public.reviews USING btree (target_id);


--
-- Name: uq_chat_rooms_pair; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_chat_rooms_pair ON public.chat_rooms USING btree (LEAST(user_a_id, user_b_id), GREATEST(user_a_id, user_b_id));


--
-- Name: chat_messages chat_messages_chat_room_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.chat_messages
    ADD CONSTRAINT chat_messages_chat_room_id_fkey FOREIGN KEY (chat_room_id) REFERENCES public.chat_rooms(id) ON DELETE CASCADE;


--
-- Name: chat_messages chat_messages_sender_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.chat_messages
    ADD CONSTRAINT chat_messages_sender_id_fkey FOREIGN KEY (sender_id) REFERENCES public.users(id);


--
-- Name: chat_rooms chat_rooms_user_a_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.chat_rooms
    ADD CONSTRAINT chat_rooms_user_a_id_fkey FOREIGN KEY (user_a_id) REFERENCES public.users(id);


--
-- Name: chat_rooms chat_rooms_user_b_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.chat_rooms
    ADD CONSTRAINT chat_rooms_user_b_id_fkey FOREIGN KEY (user_b_id) REFERENCES public.users(id);


--
-- Name: comments comments_author_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.comments
    ADD CONSTRAINT comments_author_id_fkey FOREIGN KEY (author_id) REFERENCES public.users(id);


--
-- Name: comments comments_post_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.comments
    ADD CONSTRAINT comments_post_id_fkey FOREIGN KEY (post_id) REFERENCES public.posts(id) ON DELETE CASCADE;


--
-- Name: deadline_extension_requests deadline_extension_requests_exchange_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.deadline_extension_requests
    ADD CONSTRAINT deadline_extension_requests_exchange_id_fkey FOREIGN KEY (exchange_id) REFERENCES public.exchanges(id) ON DELETE CASCADE;


--
-- Name: deadline_reminders deadline_reminders_exchange_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.deadline_reminders
    ADD CONSTRAINT deadline_reminders_exchange_id_fkey FOREIGN KEY (exchange_id) REFERENCES public.exchanges(id) ON DELETE CASCADE;


--
-- Name: disputes disputes_exchange_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.disputes
    ADD CONSTRAINT disputes_exchange_id_fkey FOREIGN KEY (exchange_id) REFERENCES public.exchanges(id) ON DELETE CASCADE;


--
-- Name: disputes disputes_filed_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.disputes
    ADD CONSTRAINT disputes_filed_by_fkey FOREIGN KEY (filed_by) REFERENCES public.users(id);


--
-- Name: disputes disputes_moderator_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.disputes
    ADD CONSTRAINT disputes_moderator_id_fkey FOREIGN KEY (moderator_id) REFERENCES public.users(id);


--
-- Name: exchange_photos exchange_photos_exchange_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.exchange_photos
    ADD CONSTRAINT exchange_photos_exchange_id_fkey FOREIGN KEY (exchange_id) REFERENCES public.exchanges(id) ON DELETE CASCADE;


--
-- Name: exchange_photos exchange_photos_uploaded_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.exchange_photos
    ADD CONSTRAINT exchange_photos_uploaded_by_fkey FOREIGN KEY (uploaded_by) REFERENCES public.users(id);


--
-- Name: exchanges exchanges_listing_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.exchanges
    ADD CONSTRAINT exchanges_listing_id_fkey FOREIGN KEY (listing_id) REFERENCES public.listings(id);


--
-- Name: exchanges exchanges_owner_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.exchanges
    ADD CONSTRAINT exchanges_owner_id_fkey FOREIGN KEY (owner_id) REFERENCES public.users(id);


--
-- Name: exchanges exchanges_reader_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.exchanges
    ADD CONSTRAINT exchanges_reader_id_fkey FOREIGN KEY (reader_id) REFERENCES public.users(id);


--
-- Name: exchanges exchanges_request_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.exchanges
    ADD CONSTRAINT exchanges_request_id_fkey FOREIGN KEY (request_id) REFERENCES public.requests(id);


--
-- Name: feedback feedback_handled_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.feedback
    ADD CONSTRAINT feedback_handled_by_fkey FOREIGN KEY (handled_by) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: feedback feedback_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.feedback
    ADD CONSTRAINT feedback_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: listing_photos listing_photos_listing_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.listing_photos
    ADD CONSTRAINT listing_photos_listing_id_fkey FOREIGN KEY (listing_id) REFERENCES public.listings(id) ON DELETE CASCADE;


--
-- Name: listings listings_book_catalog_entry_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.listings
    ADD CONSTRAINT listings_book_catalog_entry_id_fkey FOREIGN KEY (book_catalog_entry_id) REFERENCES public.book_catalog_entries(id);


--
-- Name: listings listings_hidden_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.listings
    ADD CONSTRAINT listings_hidden_by_fkey FOREIGN KEY (hidden_by) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: listings listings_owner_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.listings
    ADD CONSTRAINT listings_owner_id_fkey FOREIGN KEY (owner_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: moderation_actions moderation_actions_moderator_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.moderation_actions
    ADD CONSTRAINT moderation_actions_moderator_id_fkey FOREIGN KEY (moderator_id) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: moderation_actions moderation_actions_target_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.moderation_actions
    ADD CONSTRAINT moderation_actions_target_user_id_fkey FOREIGN KEY (target_user_id) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: notifications notifications_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: post_likes post_likes_post_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.post_likes
    ADD CONSTRAINT post_likes_post_id_fkey FOREIGN KEY (post_id) REFERENCES public.posts(id) ON DELETE CASCADE;


--
-- Name: post_likes post_likes_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.post_likes
    ADD CONSTRAINT post_likes_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: posts posts_author_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.posts
    ADD CONSTRAINT posts_author_id_fkey FOREIGN KEY (author_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: requests requests_listing_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.requests
    ADD CONSTRAINT requests_listing_id_fkey FOREIGN KEY (listing_id) REFERENCES public.listings(id) ON DELETE CASCADE;


--
-- Name: requests requests_requester_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.requests
    ADD CONSTRAINT requests_requester_id_fkey FOREIGN KEY (requester_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: restrictions restrictions_created_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.restrictions
    ADD CONSTRAINT restrictions_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: restrictions restrictions_dispute_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.restrictions
    ADD CONSTRAINT restrictions_dispute_id_fkey FOREIGN KEY (dispute_id) REFERENCES public.disputes(id);


--
-- Name: restrictions restrictions_lifted_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.restrictions
    ADD CONSTRAINT restrictions_lifted_by_fkey FOREIGN KEY (lifted_by) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: restrictions restrictions_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.restrictions
    ADD CONSTRAINT restrictions_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: reviews reviews_author_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reviews
    ADD CONSTRAINT reviews_author_id_fkey FOREIGN KEY (author_id) REFERENCES public.users(id);


--
-- Name: reviews reviews_exchange_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reviews
    ADD CONSTRAINT reviews_exchange_id_fkey FOREIGN KEY (exchange_id) REFERENCES public.exchanges(id) ON DELETE CASCADE;


--
-- Name: reviews reviews_target_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reviews
    ADD CONSTRAINT reviews_target_id_fkey FOREIGN KEY (target_id) REFERENCES public.users(id);


--
-- Name: shipment_info shipment_info_exchange_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.shipment_info
    ADD CONSTRAINT shipment_info_exchange_id_fkey FOREIGN KEY (exchange_id) REFERENCES public.exchanges(id) ON DELETE CASCADE;


--
-- Name: wishlist_items wishlist_items_book_catalog_entry_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.wishlist_items
    ADD CONSTRAINT wishlist_items_book_catalog_entry_id_fkey FOREIGN KEY (book_catalog_entry_id) REFERENCES public.book_catalog_entries(id);


--
-- Name: wishlist_items wishlist_items_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.wishlist_items
    ADD CONSTRAINT wishlist_items_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

\unrestrict U7vsfofliswd1xotb66Lwqe1tKfoe3PPMdEGgTDlyM5m84iirkBaN3ZRZKWjPul

