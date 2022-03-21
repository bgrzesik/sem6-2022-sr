#![feature(box_syntax)]
#![feature(type_alias_impl_trait)]

mod commits;
mod providers;
mod result;
mod routes;

#[rocket::launch]
fn rocket() -> _ {
    rocket::build()
        .mount("/", routes::view::get_routes())
        .mount("/api", routes::api::get_routes())
}
